package com.reflection.thecampus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class DemoFragment1 : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = TextView(context)
        view.text = "Demo Fragment 1\n\nThis is a simple test fragment with minimal content.\n\nIf this works smoothly, the issue is in your original fragments."
        view.textSize = 18f
        view.setPadding(32, 32, 32, 32)
        return view
    }
}
