package com.giz.recordcell.bmob

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.ImageView
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.Volley
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.net.SocketTimeoutException
import java.net.URLEncoder

object WebImageUtils {
    private const val TAG = "WebImageUtils"

    private const val SM_MS_BASE_URL = "https://sm.ms/api/v2/"
    private const val SM_MS_UPLOAD_URL = SM_MS_BASE_URL + "upload"
    private const val SM_MS_DELETE_URL = SM_MS_BASE_URL + "delete"
    private const val SM_MS_API_TOKEN = "01DG1wNxC2lN4K0B25zOoqKCrT8z3cUW"

    const val DEFAULT_AVATAR_URL = "https://i.loli.net/2020/01/23/9p3noKRq1WtciNf.jpg"
    const val DEFAULT_AVATAR_HASH = "JoASLnhI2debPKq7QZs8X3CNgO"
    const val DEFAULT_HEADER_BG_URL = "https://i.loli.net/2020/01/23/AP9FKf3nyzdBDOZ.jpg"
    const val DEFAULT_HEADER_BG_HASH = "GZHogrpT36wb2MYUzCExuXfcDL"

    /**
     * 把图片上传到https://sm.ms图床上，同步的
     * @param imgPath: 本地图片路径
     */
    fun uploadImage(imgPath: String): JSONObject {
        val client = OkHttpClient()

        val file = File(imgPath)
        val imgBody = RequestBody.create(MediaType.parse("image/*"), file)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("smfile", URLEncoder.encode(imgPath, "utf-8"), imgBody) // URLEncoder对中文编码
            .build()
        val request = Request.Builder()
            .addHeader("Authorization", SM_MS_API_TOKEN)
            .removeHeader("User-Agent")
            .addHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.79 Safari/537.36")
            .url(SM_MS_UPLOAD_URL)
            .post(requestBody)
            .build()
        val defaultObject = JSONObject(mapOf("success" to false, "url" to "", "hash" to ""))
        val errorMsgField = "errorMsg"
        try {
            val response = client.newCall(request).execute()
            val jsonObject = JSONObject(response.body()?.string())
            printLog(jsonObject.toString(4))
            if(jsonObject.getBoolean("success")){
                return jsonObject
            }else{
                if(jsonObject.getString("code") == "image_repeated"){
                    return jsonObject
                }
            }
        } catch (ste: SocketTimeoutException){
            defaultObject.put(errorMsgField, "请求超时")
            printLog("请求超时")
        } catch (iae: IllegalArgumentException) {
            defaultObject.put(errorMsgField, "路径中可能包含中文")
            printLog("路径中可能包含中文")
        } catch (e: Exception){
            defaultObject.put(errorMsgField, e.message)
            printLog(e.toString())
        }
        // 返回默认头像网址和hash值
        return defaultObject
    }
    fun deleteImage(hash: String) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("$SM_MS_DELETE_URL/$hash")
            .removeHeader("User-Agent")
            .addHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.79 Safari/537.36")
            .build()
        val response = client.newCall(request).execute()
        printLog(response.body()?.string() ?: "NULL Body")
    }

    private var requestQueue: RequestQueue? = null
    fun downloadImage(context: Context, url: String,
                      listener: Response.Listener<Bitmap>,
                      maxWidth: Int = 0, maxHeight: Int = 0,
                      scaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP) {
        val ir = ImageRequest(url, listener, maxWidth, maxHeight, scaleType,
            Bitmap.Config.RGB_565, object : Response.ErrorListener {
                override fun onErrorResponse(error: VolleyError?) {
                    printLog(error.toString())
                }
            })
        getVolleyRequestQueue(context).add(ir)
    }

    private fun getVolleyRequestQueue(context: Context) = requestQueue ?: Volley.newRequestQueue(context)

    private fun printLog(msg: String) = Log.d(TAG, msg)

}