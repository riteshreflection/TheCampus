package com.reflection.thecampus

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class UpdateAppBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "UpdateAppBottomSheet"
        private const val ARG_IS_FORCE_UPDATE = "is_force_update"

        fun newInstance(isForceUpdate: Boolean): UpdateAppBottomSheet {
            val fragment = UpdateAppBottomSheet()
            val args = Bundle()
            args.putBoolean(ARG_IS_FORCE_UPDATE, isForceUpdate)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_update_app, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isForceUpdate = arguments?.getBoolean(ARG_IS_FORCE_UPDATE) ?: false
        isCancelable = !isForceUpdate

        val lottieView = view.findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lottieUpdate)
        lottieView.setAnimation(R.raw.update_app)
        lottieView.playAnimation()

        val btnUpdate = view.findViewById<Button>(R.id.btnUpdate)
        btnUpdate.setOnClickListener {
            val appPackageName = requireContext().packageName
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
            } catch (e: android.content.ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
            }
        }
    }
}
