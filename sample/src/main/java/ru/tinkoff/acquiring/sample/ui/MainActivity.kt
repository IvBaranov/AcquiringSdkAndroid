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

package ru.tinkoff.acquiring.sample.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ru.tinkoff.acquiring.sample.R
import ru.tinkoff.acquiring.sample.SampleApplication
import ru.tinkoff.acquiring.sample.adapters.BooksListAdapter
import ru.tinkoff.acquiring.sample.models.Book
import ru.tinkoff.acquiring.sample.models.BooksRegistry
import ru.tinkoff.acquiring.sample.utils.SessionParams
import ru.tinkoff.acquiring.sample.utils.SettingsSdkManager
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring.Companion.EXTRA_CARD_ID
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring.Companion.RESULT_ERROR
import ru.tinkoff.acquiring.sdk.localization.AsdkSource
import ru.tinkoff.acquiring.sdk.localization.Language
import ru.tinkoff.acquiring.sdk.models.options.screen.AttachCardOptions

/**
 * @author Mariya Chernyadieva
 */
class MainActivity : AppCompatActivity(), BooksListAdapter.BookDetailsClickListener {

    private lateinit var listViewBooks: ListView
    private lateinit var adapter: BooksListAdapter
    private lateinit var settings: SettingsSdkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        settings = SettingsSdkManager(this)

        val booksRegistry = BooksRegistry()
        adapter = BooksListAdapter(this, booksRegistry.getBooks(this), this)
        initViews()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        if (!settings.isFpsEnabled) {
            menu.findItem(R.id.menu_action_static_qr).isVisible = false
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        invalidateOptionsMenu()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_action_cart -> {
                CartActivity.start(this)
                true
            }
            R.id.menu_action_attach_card -> {
                openAttachCardScreen()
                true
            }
            R.id.menu_action_about -> {
                AboutActivity.start(this)
                true
            }
            R.id.menu_action_static_qr -> {
                openStaticQrScreen()
                true
            }
            R.id.menu_action_settings -> {
                SettingsActivity.start(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            STATIC_QR_REQUEST_CODE -> {
                if (resultCode == RESULT_ERROR) {
                    Toast.makeText(this, R.string.attachment_failed, Toast.LENGTH_SHORT).show()
                }
            }
            ATTACH_CARD_REQUEST_CODE -> {
                when (resultCode) {
                    RESULT_OK -> PaymentResultActivity.start(this, data?.getStringExtra(EXTRA_CARD_ID)!!)
                    RESULT_CANCELED -> Toast.makeText(this, R.string.attachment_cancelled, Toast.LENGTH_SHORT).show()
                    RESULT_ERROR -> Toast.makeText(this, R.string.attachment_failed, Toast.LENGTH_SHORT).show()
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onBookDetailsClicked(book: Book) {
        DetailsActivity.start(this, book)
    }

    private fun initViews() {
        listViewBooks = findViewById(R.id.lv_books)
        listViewBooks.adapter = adapter
    }

    private fun openAttachCardScreen() {
        val settings = SettingsSdkManager(this)
        val params = SessionParams[settings.terminalKey]

        val options = AttachCardOptions()
                .setOptions {
                    customerOptions {
                        customerKey = params.customerKey
                        checkType = settings.checkType
                        email = params.customerEmail
                    }
                    featuresOptions {
                        useSecureKeyboard = settings.isCustomKeyboardEnabled
                        cameraCardScanner = settings.cameraScanner
                        darkThemeMode = settings.resolveDarkThemeMode()
                        theme = settings.resolveAttachCardStyle()
                    }
                }

        SampleApplication.tinkoffAcquiring.openAttachCardScreen(this, options, ATTACH_CARD_REQUEST_CODE)
    }

    private fun openStaticQrScreen() {
        SampleApplication.tinkoffAcquiring.openStaticQrScreen(this, AsdkSource(Language.RU), STATIC_QR_REQUEST_CODE)
    }

    companion object {

        private const val ATTACH_CARD_REQUEST_CODE = 11
        private const val STATIC_QR_REQUEST_CODE = 12
    }
}
