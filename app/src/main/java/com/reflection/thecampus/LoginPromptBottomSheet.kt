package com.reflection.thecampus

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class LoginPromptBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_login_prompt, container, false)

        view.findViewById<View>(R.id.btnLogin).setOnClickListener {
            startActivity(Intent(context, LoginActivity::class.java))
            dismiss()
        }

        view.findViewById<View>(R.id.btnSignup).setOnClickListener {
            startActivity(Intent(context, SignupActivity::class.java))
            dismiss()
        }

        return view
    }

    companion object {
        const val TAG = "LoginPromptBottomSheet"
    }
}
