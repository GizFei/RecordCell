package com.giz.recordcell.helpers

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import java.io.File

class FileUriUtils {
    companion object {
        fun getFilePathByUri(context: Context, uri: Uri?): String?{
            printUri(uri)
            uri ?: return null
            var path: String? = null
            // 以file://开头
            if(ContentResolver.SCHEME_FILE == uri.scheme){
                path = uri.path
            }else{
                // 以content://开头
                if(DocumentsContract.isDocumentUri(context, uri)){
                    if(isExternalStorageDocument(uri)){
                        // ExternalStorageProvider, [0]type,[1]id
                        val docId = DocumentsContract.getDocumentId(uri).split(":")
                        if(docId[0].equals("primary", true)){
                            path = "${Environment.getExternalStorageDirectory()}/${docId[1]}"
                        }
                    }else if(isDownloadsDocument(uri)){
                        // DownloadsProvider
                        val id = DocumentsContract.getDocumentId(uri)
                        val contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                            id.toLong())
                        path = getDataColumn(context, contentUri, null, null)
                    }else if(isMediaDocument(uri)){
                        // MediaProvider
                        val docId = DocumentsContract.getDocumentId(uri).split(":")
                        val type = docId[0]
                        val id = docId[1]
                        val contentUri: Uri? = when(type){
                            "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            else -> null
                        }
                        val selection = "${MediaStore.MediaColumns._ID}=?"
                        val selectionArgs = arrayOf(id)
                        path = getDataColumn(context, contentUri, selection, selectionArgs)
                    }
                }else if(isMiuiGalleryDocument(uri)){
                    // 小米手机相册
                    path = uri.path?.replace("/raw/", "")
                }
            }
            return path
        }

        private fun isExternalStorageDocument(uri: Uri): Boolean =
            "com.android.externalstorage.documents" == uri.authority

        private fun isDownloadsDocument(uri: Uri): Boolean =
            "com.android.providers.downloads.documents" == uri.authority

        private fun isMediaDocument(uri: Uri): Boolean =
            "com.android.providers.media.documents" == uri.authority

        // 小米手机相册
        private fun isMiuiGalleryDocument(uri: Uri): Boolean =
            "com.miui.gallery.open" == uri.authority

        private fun getDataColumn(context: Context,
                                  uri: Uri?, selection: String?,
                                  selectionArgs: Array<String>?): String? {
            uri ?: return null
            var cursor: Cursor? = null
            val column = MediaStore.MediaColumns.DATA
            val projection = arrayOf(column)
            try {
                cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
                if(cursor != null && cursor.moveToFirst()){
                    val columnIndex = cursor.getColumnIndex(column)
                    val res = cursor.getString(columnIndex)
                    cursor.close()
                    return res
                }
            }finally {
                cursor?.close()
            }
            return null
        }

        /**
         * 判断文件大小
         * @param path 文件路径
         * @param mb 文件大小上限，单位MB
         */
        fun judgeFileSize(path: String, mb: Int): Boolean{
            val file = File(path)
            printLog("文件大小：${file.length()}")
            return file.length() <= mb * 1000 * 1000
        }

        fun printUri(uri: Uri?){
            if(uri == null){
                printLog("空Uri")
                return
            }
            uri.pathSegments.forEach { printLog("pathSegment: $it") }
            Log.d("FileUriUtils","""
                    文件Uri信息：path: ${uri.path},
                    scheme: ${uri.scheme},
                    authority: ${uri.authority},
                    host: ${uri.host}, 
                    port: ${uri.port}
                    encodePath: ${uri.encodedPath}, 
                    query: ${uri.encodedQuery},
                """.trimIndent())
        }

        private fun printLog(msg: String) = Log.d("FileUriUtils", msg)
    }
}