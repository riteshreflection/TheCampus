package com.reflection.thecampus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class DemoFragment2 : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = TextView(context)
        view.text = "Demo Fragment 2\n\nAnother simple test fragment.\n\nTap tabs to test switching speed."
        view.textSize = 18f
        view.setPadding(32, 32, 32, 32)
        view.setBackgroundColor(0xFFF5F5F5.toInt())
        return view
    }
}
