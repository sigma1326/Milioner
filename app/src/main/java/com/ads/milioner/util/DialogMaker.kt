package com.ads.milioner.util

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.ads.milioner.R
import kotlinx.android.synthetic.main.phone_input_dialog.view.*

class DialogMaker {
    companion object {
        fun phoneInputDialog(
            context: Context,
            phone: String,
            onPhoneEntered: OnPhoneEnteredListener

        ) {
            val builder = AlertDialog.Builder(context)

            val view = LayoutInflater.from(context).inflate(R.layout.phone_input_dialog, null)
            builder.setView(view)

            view?.et_code?.setText(phone)

            val alertDialog = builder.create()
            alertDialog.setCancelable(false)
            alertDialog.show()

            view.confirm_btn.setOnClickListener {
                alertDialog.dismiss()
                onPhoneEntered.onPhoneEntered(view?.et_code?.text.toString(), false)
            }

            view.cancel_btn.setOnClickListener {
                alertDialog.dismiss()
                onPhoneEntered.onPhoneEntered(phone, true)
            }


        }
    }

    public interface OnPhoneEnteredListener {
        fun onPhoneEntered(phone: String, cancel: Boolean)
    }
}