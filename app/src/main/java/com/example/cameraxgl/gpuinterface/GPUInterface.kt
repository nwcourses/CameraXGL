package com.example.cameraxgl.gpuinterface

import android.opengl.GLES20
import java.nio.Buffer
import android.util.Log

// Controls the interface between CPU and GPU, i.e. all the interfacing with shaders
// and associating buffer data with shader variables.

class GPUInterface(vertexShaderCode: String, fragmentShaderCode: String) {

    private var vertexShader = -1
    private var fragmentShader = -1
    private var shaderProgram = -1

    init {
        vertexShader = addVertexShader(vertexShaderCode)
        if (vertexShader >= 0) {
            fragmentShader = addFragmentShader(fragmentShaderCode)
            if (fragmentShader >= 0) {
                shaderProgram = makeProgram(vertexShader, fragmentShader)
            }
        }
    }


    fun drawBufferedData(
        vertices: Buffer, stride: Int, attrVar: String,
        vertexStart: Int, nVertices: Int
    ) {
        if (isValid()) {
            val attrVarRef = getShaderVarRef(attrVar)
            vertices.position(0)

            GLES20.glEnableVertexAttribArray(attrVarRef)
            GLES20.glVertexAttribPointer(attrVarRef, 3, GLES20.GL_FLOAT, false, stride, vertices)

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, vertexStart, nVertices)

        }
    }

    fun drawIndexedBufferedData(
        vertices: Buffer, indices: Buffer, stride: Int,
        attrVar: String
    ) {
        if (isValid()) {
            val attrVarRef = getShaderVarRef(attrVar)
            vertices.position(0)
            indices.position(0)

            GLES20.glEnableVertexAttribArray(attrVarRef)
            GLES20.glVertexAttribPointer(attrVarRef, 3, GLES20.GL_FLOAT, false, stride, vertices)

            GLES20.glDrawElements(
                GLES20.GL_TRIANGLES,
                indices.limit(),
                GLES20.GL_UNSIGNED_SHORT,
                indices
            )

        }
    }

    private fun getShaderVarRef(shaderVar: String): Int {
        return if (isValid()) GLES20.glGetAttribLocation(shaderProgram, shaderVar) else -1
    }


    // could be used e.g. for sending texture id
    fun setUniform1i(shaderVar: String, i: Int) {
        if (isValid()) {
            val refShaderVar = GLES20.glGetUniformLocation(shaderProgram, shaderVar)
            GLES20.glUniform1i(refShaderVar, i)
        }
    }


    fun select() {
        GLES20.glUseProgram(shaderProgram)
    }

    private fun isValid(): Boolean {
        return shaderProgram >= 0
    }
}

fun addVertexShader(shaderCode: String): Int {
    return getShader(GLES20.GL_VERTEX_SHADER, shaderCode)
}

fun addFragmentShader(shaderCode: String): Int {
    return getShader(GLES20.GL_FRAGMENT_SHADER, shaderCode)
}

fun getShader(shaderType: Int, shaderCode: String): Int {
    val shader = GLES20.glCreateShader(shaderType)
    GLES20.glShaderSource(shader, shaderCode)
    GLES20.glCompileShader(shader)
    val compileStatus = IntArray(1)
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
    if (compileStatus[0] == 0) {
        Log.e("OpenGL", "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader))
        GLES20.glDeleteShader(shader)
        return -1
    }
    return shader
}

fun makeProgram(vertexShader: Int, fragmentShader: Int): Int {
    val shaderProgram = GLES20.glCreateProgram()
    GLES20.glAttachShader(shaderProgram, vertexShader)
    GLES20.glAttachShader(shaderProgram, fragmentShader)
    GLES20.glLinkProgram(shaderProgram)
    val linkStatus = IntArray(1)
    GLES20.glGetProgramiv(shaderProgram, GLES20.GL_LINK_STATUS, linkStatus, 0)
    if (linkStatus[0] == 0) {
        Log.e(
            "OpenGL",
            "Error linking shader program: " + GLES20.glGetProgramInfoLog(shaderProgram)
        )
        GLES20.glDeleteProgram(shaderProgram)
        return -1
    }
    GLES20.glUseProgram(shaderProgram)
    return shaderProgram
}

fun setupTexture(textureId: Int) {
    GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
}

