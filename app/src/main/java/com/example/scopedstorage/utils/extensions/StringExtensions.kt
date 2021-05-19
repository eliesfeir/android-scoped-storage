package com.example.scopedstorage.utils.extensions

fun String.getFileExtension(): String {
    val filenameArray = this.split("\\.").toTypedArray()
    return filenameArray[filenameArray.size - 1]
}