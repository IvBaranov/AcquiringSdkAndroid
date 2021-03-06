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

package ru.tinkoff.acquiring.sdk.ui.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.adapters.CardListAdapter
import ru.tinkoff.acquiring.sdk.localization.AsdkLocalization
import ru.tinkoff.acquiring.sdk.localization.LocalizationResources
import ru.tinkoff.acquiring.sdk.models.*
import ru.tinkoff.acquiring.sdk.models.options.screen.AttachCardOptions
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.ui.customview.BottomContainer
import ru.tinkoff.acquiring.sdk.ui.customview.NotificationDialog
import ru.tinkoff.acquiring.sdk.viewmodel.SavedCardsViewModel

/**
 * @author Mariya Chernyadieva
 */
internal class SavedCardsActivity : BaseAcquiringActivity(), CardListAdapter.OnMoreIconClickListener {

    private lateinit var deletingBottomContainer: BottomContainer
    private lateinit var cardListView: ListView

    private lateinit var paymentOptions: PaymentOptions
    private lateinit var localization: LocalizationResources
    private lateinit var cardsAdapter: CardListAdapter
    private lateinit var viewModel: SavedCardsViewModel

    private var deletingConfirmDialog: AlertDialog? = null
    private var notificationDialog: NotificationDialog? = null

    private var isDeletingDialogShowing = false
    private var isDeletingBottomContainerShowed = false
    private var deletingCard: Card? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acq_activity_saved_cards)

        localization = AsdkLocalization.resources
        paymentOptions = options as PaymentOptions

        savedInstanceState?.let {
            isDeletingDialogShowing = it.getBoolean(STATE_DELETING_DIALOG_SHOWING)
            isDeletingBottomContainerShowed = it.getBoolean(STATE_BOTTOM_CONTAINER_SHOWING)
            deletingCard = it.getSerializable(STATE_DELETING_CARD) as Card?
        }

        initViews()

        viewModel = provideViewModel(SavedCardsViewModel::class.java) as SavedCardsViewModel
        observeLiveData()

        loadCards()

        if (isDeletingDialogShowing && deletingCard != null) {
            showDeletingConfirmDialog(deletingCard!!)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ATTACH_CARD_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                loadCards()
                setCardsChangedResult()
                notificationDialog = NotificationDialog(this@SavedCardsActivity).apply {
                    show()
                    showSuccess(localization.addCardDialogSuccessCardAdded)
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putBoolean(STATE_DELETING_DIALOG_SHOWING, isDeletingDialogShowing)
            putBoolean(STATE_BOTTOM_CONTAINER_SHOWING, isDeletingBottomContainerShowed)
            putSerializable(STATE_DELETING_CARD, deletingCard)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        deletingConfirmDialog?.dismiss()
        notificationDialog?.dismiss()
    }

    override fun onBackPressed() {
        if (deletingBottomContainer.isShowed) {
            deletingBottomContainer.hide()
        } else {
            super.onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onMoreIconClick(card: Card) {
        deletingCard = card
        deletingBottomContainer.show()
    }

    private fun initViews() {
        val toolbar = findViewById<Toolbar>(R.id.acq_toolbar)
        toolbar.title = localization.cardListTitle
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        deletingBottomContainer = findViewById(R.id.acq_bottom_container)
        deletingBottomContainer.run {
            showInitAnimation = false
            containerState = if (isDeletingBottomContainerShowed) {
                BottomContainer.STATE_SHOWED
            } else {
                BottomContainer.STATE_HIDDEN
            }
            setContainerStateListener(object : BottomContainer.ContainerStateListener {
                override fun onHidden() {
                    isDeletingBottomContainerShowed = false
                }

                override fun onShowed() {
                    isDeletingBottomContainerShowed = true
                }

                override fun onFullscreenOpened() = Unit
            })
        }

        val addCardTextView = findViewById<TextView>(R.id.acq_add_card)
        addCardTextView.text = localization.addCardAttachmentTitle
        addCardTextView.setOnClickListener {
            openAttachActivity()
        }

        val deleteCardTextView = findViewById<TextView>(R.id.acq_delete_card)
        deleteCardTextView.text = localization.cardListDeleteCard
        deleteCardTextView.setOnClickListener {
            showDeletingConfirmDialog(deletingCard!!)
        }

        cardListView = findViewById(R.id.acq_card_list)
        cardsAdapter = CardListAdapter(this)
        cardsAdapter.moreClickListener = this
        cardListView.adapter = cardsAdapter
    }

    private fun openAttachActivity() {
        val options = AttachCardOptions().setOptions {
            setTerminalParams(paymentOptions.terminalKey, paymentOptions.password, paymentOptions.publicKey)
            customerOptions {
                checkType = paymentOptions.customer.checkType
                customerKey = paymentOptions.customer.customerKey
            }
            features = paymentOptions.features
        }
        val intent = createIntent(this, options, AttachCardActivity::class.java)
        startActivityForResult(intent, ATTACH_CARD_REQUEST_CODE)
    }

    private fun observeLiveData() {
        with(viewModel) {
            loadStateLiveData.observe(this@SavedCardsActivity, Observer { handleLoadState(it) })
            screenStateLiveData.observe(this@SavedCardsActivity, Observer { handleScreenState(it) })
            cardsResultLiveData.observe(this@SavedCardsActivity, Observer {
                cardsAdapter.setCards(it)
            })
            deleteCardEventLiveData.observe(this@SavedCardsActivity, Observer {
                it.getValueIfNotHandled()?.let {
                    loadCards()
                    setCardsChangedResult()
                    notificationDialog = NotificationDialog(this@SavedCardsActivity).apply {
                        show()
                        showSuccess(String.format(localization.addCardDialogSuccessCardDeleted!!,
                                cardsAdapter.getLastPanNumbers(deletingCard!!.pan!!)))
                    }
                }
            })
        }
    }

    private fun setCardsChangedResult() {
        val intent = Intent()
        intent.putExtra(RESULT_CARDS_CHANGED, true)
        setResult(Activity.RESULT_OK, intent)
    }

    private fun loadCards() {
        viewModel.getCardList(paymentOptions.customer.customerKey)
    }

    private fun handleScreenState(screenState: ScreenState) {
        when (screenState) {
            is ErrorButtonClickedEvent -> loadCards()
            is FinishWithErrorScreenState -> showErrorScreen(localization.payDialogErrorFallbackMessage!!)
            is ErrorScreenState -> showError(screenState.message)
        }
    }

    private fun showError(message: String) {
        showErrorScreen(message) {
            viewModel.createEvent(ErrorButtonClickedEvent)
        }
    }

    private fun showDeletingConfirmDialog(card: Card) {
        deletingConfirmDialog = AlertDialog.Builder(this).apply {
            setTitle(String.format(localization.cardListDialogDeleteTitleFormat!!, cardsAdapter.getLastPanNumbers(card.pan!!)))
            setMessage(localization.cardListDialogDeleteMessage)
            setPositiveButton(localization.cardListDelete) { dialog, _ ->
                dialog.dismiss()
                viewModel.deleteCard(card.cardId!!, paymentOptions.customer.customerKey)
                deletingBottomContainer.hide()
                isDeletingDialogShowing = false
            }
            setNegativeButton(localization.commonCancel) { dialog, _ ->
                dialog.dismiss()
                deletingBottomContainer.hide()
                isDeletingDialogShowing = false
            }
            setOnCancelListener {
                isDeletingDialogShowing = false
            }
        }.show()
        isDeletingDialogShowing = true
    }

    companion object {

        const val RESULT_CARDS_CHANGED = "cards_changed"

        private const val STATE_DELETING_DIALOG_SHOWING = "state_dialog"
        private const val STATE_BOTTOM_CONTAINER_SHOWING = "state_bottom_container"
        private const val STATE_DELETING_CARD = "state_card"

        private const val ATTACH_CARD_REQUEST_CODE = 50
    }
}