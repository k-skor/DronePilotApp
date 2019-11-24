package com.example.dronepilotapp.matrix

import com.example.dronepilotapp.log.LogUtil
import kotlin.math.sqrt

class MatrixCalculations {

    companion object {

        fun multiply(matrixA: FloatArray, matrixB: FloatArray): FloatArray {
            require(matrixA.size == matrixB.size) { "Only equal size matrices allowed." }
            require(matrixA.isNotEmpty()) { "Matrix size cannot be 0." }
            val rankF = sqrt(matrixA.size.toFloat())
            require(rankF % 1 == 0f) { "Only square matrices allowed." }

            val rank = rankF.toInt()
            val product = FloatArray(rank*rank)

            for (i in 0 until rank) {
                for (j in 0 until rank) {
                    val sum = FloatArray(rank)
                    for (k in 0 until rank) {
                        sum[j] += matrixA[k+i*rank] * matrixB[j+k*rank]
                    }
                    product[j+i*rank] += sum[j]
                }
            }

            return product
        }

        fun transpose(matrix: FloatArray): FloatArray {
            require(matrix.isNotEmpty()) { "Matrix size cannot be 0." }
            val rankF = sqrt(matrix.size.toFloat())
            require(rankF % 1 == 0f) { "Only square matrices allowed." }

            val rank = rankF.toInt()
            val product = FloatArray(matrix.size)

            for (i in 0 until rank) {
                for (j in 0 until rank) {
                    product[j+i*rank] = matrix[i+j*rank]
                }
            }
            return product
        }

        fun createUnit(rank: Int): FloatArray {
            val product = FloatArray(rank*rank)
            for (i in 0 until rank) {
                for (j in 0 until rank) {
                    product[j+i*rank] = if (i == j) 1f else 0f
                }
            }
            return product
        }

        fun copy(matrixFrom: FloatArray, matrixTo: FloatArray) {
            val rankFFrom = sqrt(matrixFrom.size.toFloat())
            val rankFTo = sqrt(matrixTo.size.toFloat())
            require((rankFFrom % 1 == 0f) || (rankFTo % 1 == 0f)) { "Only square matrices allowed." }

            val rankFrom = rankFFrom.toInt()
            val rankTo = rankFTo.toInt()
            val rank = Math.min(rankFrom, rankTo)

            for (i in 0 until rank) {
                for (j in 0 until rank) {
                    matrixTo[j+i*rankTo] = matrixFrom[j+i*rankFrom]
                }
            }
        }
    }
}