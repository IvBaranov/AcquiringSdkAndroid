/*
 * Copyright © 2020 Tinkoff Bank
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ru.tinkoff.acquiring.sdk.network

import ru.tinkoff.acquiring.sdk.AcquiringSdk

/**
 * Содержит константы для создания запросов к Acquiring API
 *
 * @author Mariya Chernyadieva
 */
object AcquiringApi {

    const val ADD_CARD_METHOD = "AddCard"
    const val ATTACH_CARD_METHOD = "AttachCard"
    const val CHARGE_METHOD = "Charge"
    const val FINISH_AUTHORIZE_METHOD = "FinishAuthorize"
    const val GET_ADD_CARD_STATE_METHOD = "GetAddCardState"
    const val CHECK_3DS_VERSION_METHOD = "Check3dsVersion"
    const val GET_CARD_LIST_METHOD = "GetCardList"
    const val GET_STATE_METHOD = "GetState"
    const val INIT_METHOD = "Init"
    const val REMOVE_CARD_METHOD = "RemoveCard"
    const val SUBMIT_RANDOM_AMOUNT_METHOD = "SubmitRandomAmount"
    const val GET_QR_METHOD = "GetQr"
    const val GET_STATIC_QR_METHOD = "GetStaticQr"
    const val SUBMIT_3DS_AUTHORIZATION = "Submit3DSAuthorization"
    const val SUBMIT_3DS_AUTHORIZATION_V2 = "Submit3DSAuthorizationV2"
    const val COMPLETE_3DS_METHOD_V2 = "Complete3DSMethodv2"

    const val API_ERROR_CODE_3DSV2_NOT_SUPPORTED = "106"
    const val API_ERROR_CODE_ACQUIRING_EXCEPTION = "3"

    const val RECURRING_TYPE_KEY = "recurringType"
    const val RECURRING_TYPE_VALUE = "12"
    const val FAIL_MAPI_SESSION_ID = "failMapiSessionId"

    internal const val STREAM_BUFFER_SIZE = 4096
    internal const val API_REQUEST_METHOD_POST = "POST"

    internal const val JSON = "application/json"
    internal const val FORM_URL_ENCODED = "application/x-www-form-urlencoded"
    internal const val TIMEOUT = 40000

    internal val performedErrorCodesList = listOf("0", "104")

    private const val API_URL_RELEASE_OLD = "https://securepay.tinkoff.ru/rest"
    private const val API_URL_DEBUG_OLD = "https://rest-api-test.tcsbank.ru/rest"

    private const val API_VERSION = "v2"
    private const val API_URL_RELEASE = "https://securepay.tinkoff.ru/$API_VERSION"
    private const val API_URL_DEBUG = "https://rest-api-test.tcsbank.ru/$API_VERSION"

    private val oldMethodsList = listOf("Submit3DSAuthorization")

    /**
     * Возвращает базовый Url для переданного названия метода запроса.
     * Зависит от режима работы SDK [AcquiringSdk.isDeveloperMode]
     */
    fun getUrl(apiMethod: String): String {
        return if (useV1Api(apiMethod)) {
            if (AcquiringSdk.isDeveloperMode) API_URL_DEBUG_OLD else API_URL_RELEASE_OLD
        } else {
            if (AcquiringSdk.isDeveloperMode) API_URL_DEBUG else API_URL_RELEASE
        }
    }

    internal fun useV1Api(apiMethod: String): Boolean {
        return oldMethodsList.contains(apiMethod)
    }
}