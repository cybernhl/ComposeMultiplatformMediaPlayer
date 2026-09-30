@file:Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
@file:OptIn(ExperimentalWasmJsInterop::class)

package chaintech.videoplayer.util


import org.w3c.dom.HTMLMediaElement
import kotlin.js.Promise

external object ShakaWasmHelpers {
    fun setABR(player: JsAny, enabled: Boolean)
    fun createEmptyObject(): JsAny
    fun setObjectProperty(obj: JsAny, key: String, value: String)
    fun configureHeaders(player: JsAny, headers: JsAny)
    fun setClearKey(player: JsAny, keyId: String, key: String)
    fun requestFullscreen(element: org.w3c.dom.HTMLElement)
    fun exitFullscreen()
}


external object shaka : JsAny {
    object polyfill : JsAny {
        fun installAll()
    }


    class Player(mediaElement: HTMLMediaElement) : JsAny {
        fun load(uri: String): Promise<JsAny?>
        fun unload(): Promise<JsAny?>
        fun destroy(): Promise<JsAny?>
        fun addEventListener(event: String, listener: (ShakaError) -> Unit)

        fun getVariantTracks(): JsArray<ShakaVariantTrack>
        fun selectVariantTrack(track: ShakaVariantTrack, clearBuffer: Boolean, safeSwitch: Boolean)
        fun configure(config: JsAny)

        fun getAudioLanguagesAndRoles(): JsArray<ShakaAudioTrack>
        fun selectAudioLanguage(language: String, role: String = definedExternally)

        fun getTextTracks(): JsArray<ShakaTextTrack>
        fun selectTextLanguage(language: String, role: String = definedExternally)
        fun setTextTrackVisibility(visible: Boolean)
    }
}

external interface ShakaError : JsAny {
    val detail: ShakaErrorDetail
}

external interface ShakaErrorDetail : JsAny {
    val code: Int
    val message: String
}

external interface ShakaVariantTrack : JsAny {
    val id: Int
    val active: Boolean
    val width: Int
    val height: Int
    val bandwidth: Int
    val language: String
    val label: String?
}

external interface ShakaAudioTrack : JsAny {
    val id: Int
    val language: String
    val label: String?
    val active: Boolean
}

external interface ShakaTextTrack : JsAny {
    val id: Int
    val language: String
    val label: String?
    val kind: String
    val active: Boolean
}

fun createHeadersObject(headers: Map<String, String>?): JsAny {
    val jsObj = ShakaWasmHelpers.createEmptyObject()
    headers?.forEach { (key, value) ->
        ShakaWasmHelpers.setObjectProperty(jsObj, key, value)
    }
    return jsObj
}