/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan

import llvm.*
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.types.KotlinType

internal fun KonanSymbols.getTypeConversion(
        actualType: KotlinType,
        expectedType: KotlinType
): IrSimpleFunctionSymbol? {
    val actualValueType = actualType.correspondingValueType
    val expectedValueType = expectedType.correspondingValueType

    return when {
        actualValueType == expectedValueType -> null

        actualValueType == null && expectedValueType != null -> {
            // This may happen in the following cases:
            // 1.  `actualType` is `Nothing`;
            // 2.  `actualType` is incompatible.

            this.getUnboxFunction(expectedValueType)
        }

        actualValueType != null && expectedValueType == null -> {
            this.boxFunctions[actualValueType]!!
        }

        else -> throw IllegalArgumentException("actual type is $actualType, expected $expectedType")
    }
}

internal fun KonanSymbols.getUnboxFunction(valueType: ValueType): IrSimpleFunctionSymbol =
        this.unboxFunctions[valueType]
                ?: this.boxClasses[valueType]!!.getPropertyGetter("value")!! as IrSimpleFunctionSymbol


// Static boxing implementation
/**
 * Represents static array of boxes
 */
internal enum class BoxCache(
        val valueType: ValueType,
        val getIntrinsic: String,
        val checkIntrinsic: String
) {
    BYTE(ValueType.BYTE, "konan.internal.getCachedByteBox", "konan.internal.inByteBoxCache"),
    SHORT(ValueType.SHORT, "konan.internal.getCachedShortBox", "konan.internal.inShortBoxCache"),
    CHAR(ValueType.CHAR, "konan.internal.getCachedCharBox", "konan.internal.inCharBoxCache"),
    INT(ValueType.INT, "konan.internal.getCachedIntBox", "konan.internal.inIntBoxCache"),
    LONG(ValueType.LONG, "konan.internal.getCachedLongBox", "konan.internal.inLongBoxCache");

    val cacheName = "${valueType.name}Boxes"

    val rangeStartName = "${valueType.name}RangeStart"
    val rangeEndName = "${valueType.name}RangeEnd"

    companion object {
        /**
         * returns cache corresponding to the given getIntrinsic name
         */
        fun getCacheByBoxGetter(getBoxMethodName: String): BoxCache? =
            BoxCache.values().firstOrNull { it.getIntrinsic == getBoxMethodName }

        /**
         * returns cache corresponding to the given checkIntrinsic name
         */
        fun getCacheByInRangeChecker(checkRangeMethodName: String): BoxCache? =
            BoxCache.values().firstOrNull { it.checkIntrinsic == checkRangeMethodName }

        /**
         * Initialize globals
         */
        fun initialize(context: Context) {
            values().forEach {
                it.initRange(context)
                it.initCache(context)
            }
        }
    }
}

/**
 * Adds global that refers to the cache
 * If output target is native binary then the cache is created
 */
private fun BoxCache.initCache(context: Context): LLVMValueRef {
    return if (context.config.produce.isNativeBinary) {
        context.llvm.staticData.createBoxes(this)
    } else {
        context.llvm.staticData.addGlobal(cacheName, context.llvm.runtime.objHeaderPtrType, false)
    }
}

/**
 * Creates globals that defines the smallest and the biggest cached values
 */
private fun BoxCache.initRange(context: Context) {
    if (context.config.produce.isNativeBinary) {
        val (start, end) = getRange(context)
        context.llvm.staticData.placeGlobal(rangeStartName, createConstant(start), true)
        context.llvm.staticData.placeGlobal(rangeEndName, createConstant(end), true)
    } else {
        context.llvm.staticData.addGlobal(rangeStartName, llvmType, false)
        context.llvm.staticData.addGlobal(rangeEndName, llvmType, false)
    }
}

private fun BoxCache.llvmRange(context: Context): Pair<LLVMValueRef, LLVMValueRef> {
    val start = LLVMGetNamedGlobal(context.llvmModule, rangeStartName)!!
    val end = LLVMGetNamedGlobal(context.llvmModule, rangeEndName)!!
    return Pair(start, end)
}

/**
 * Checks that box for the given [value] is in the cache
 */
internal fun BoxCache.inRange(codegen: FunctionGenerationContext, value: LLVMValueRef): LLVMValueRef {
    val (startPtr, endPtr) = llvmRange(codegen.context)
    val start = codegen.load(startPtr)
    val end = codegen.load(endPtr)
    val startCheck = codegen.icmpGe(value, start)
    val endCheck = codegen.icmpLe(value, end)
    return codegen.and(startCheck, endCheck)
}

private fun BoxCache.getRange(context: Context) = context.config.target.getBoxCacheRange(valueType)

internal fun BoxCache.getCachedValue(codegen: FunctionGenerationContext, value: LLVMValueRef): LLVMValueRef {
    val startPtr = llvmRange(codegen.context).first
    val start = codegen.load(startPtr)
    // we should subtract range start to get index of the box
    val index = LLVMBuildSub(codegen.builder, value, start, "offset")!!
    val cache = LLVMGetNamedGlobal(codegen.context.llvmModule, cacheName)!!
    val boxPointer = codegen.gep(cache, index)
    return codegen.load(boxPointer)
}

private fun StaticData.createBoxes(box: BoxCache): LLVMValueRef {
    val kotlinType = context.ir.symbols.boxClasses[box.valueType]!!.descriptor.defaultType
    val (start, end) = box.getRange(context)
    val values = (start..end).map { createKotlinObject(kotlinType, box.createConstant(it)) }
    return placeGlobalConstArray(box.cacheName, context.llvm.runtime.objHeaderPtrType, values, true).llvm
}

private fun BoxCache.createConstant(value: Int) =
    constValue(when (valueType) {
        ValueType.BYTE  -> LLVMConstInt(LLVMInt8Type(),  value.toByte().toLong(),  1)!!
        ValueType.CHAR  -> LLVMConstInt(LLVMInt16Type(), value.toChar().toLong(),  0)!!
        ValueType.SHORT -> LLVMConstInt(LLVMInt16Type(), value.toShort().toLong(), 1)!!
        ValueType.INT   -> LLVMConstInt(LLVMInt32Type(), value.toLong(),   1)!!
        ValueType.LONG  -> LLVMConstInt(LLVMInt64Type(), value.toLong(),             1)!!
        else            -> error("Cannot box value of type $valueType")
    })

private val BoxCache.llvmType
    get() = when (valueType) {
        ValueType.BYTE -> LLVMInt8Type()!!
        ValueType.CHAR -> LLVMInt16Type()!!
        ValueType.SHORT -> LLVMInt16Type()!!
        ValueType.INT -> LLVMInt32Type()!!
        ValueType.LONG -> LLVMInt64Type()!!
        else            -> error("Cannot box value of type $valueType")
    }

private val defalutCacheRange = mapOf(
        ValueType.BYTE  to (-128 to 128),
        ValueType.SHORT to (-128 to 127),
        ValueType.CHAR  to (0 to 255),
        ValueType.INT   to (-128 to 127),
        ValueType.LONG  to (-128 to 127)
)

// TODO: add convenient way to override default range
fun KonanTarget.getBoxCacheRange(valueType: ValueType): Pair<Int, Int> = when (this) {
    else -> defalutCacheRange[valueType]!!
}