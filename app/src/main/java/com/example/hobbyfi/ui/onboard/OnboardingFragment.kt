package com.example.hobbyfi.ui.onboard

import androidx.fragment.app.Fragment
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentOnboardingBinding
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.ui.auth.AuthActivity
import com.example.hobbyfi.ui.base.BaseFragment
import com.facebook.login.LoginManager
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.kodein
import org.kodein.di.generic.instance
import java.io.ByteArrayOutputStream

class OnboardingFragment : Fragment(), KodeinAware {
    override val kodein: Kodein by kodein()
    private val prefConfig: PrefConfig by instance(tag = "prefConfig")

    interface OnPageContinueListener {
        fun onPageChanged()
    }

    private var onPageChangedListener: OnPageContinueListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding = FragmentOnboardingBinding.inflate(layoutInflater, container, false)

        val args = requireArguments()

        with(binding) {
            bottomText.text = args.getString(BOTTOM_TEXT)
            topText.text = args.getString(TOP_TEXT)
            args.getString(EXTRA_TEXT)?.let {
                extraText.isVisible = true
                extraText.text = it
            }
            args.getByteArray(CENTER_DRAWABLE)?.let {
                centerIcon.setImageDrawable(BitmapDrawable(resources, BitmapFactory.decodeByteArray(it, 0, it.size)))
            }

            with(continueText) {
                if(args.getBoolean(LAST_FRAGMENT)) {
                    text = getString(R.string.finish)
                    setOnClickListener {
                        // start auth activity
                        prefConfig.writeOnboardingValid(false)
                        LoginManager.getInstance().logOut()
                        startActivity(Intent(requireContext(), AuthActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        })
                        requireActivity().finish()
                    }
                } else {
                    text = getString(R.string.cont)
                    setOnClickListener {
                        // continue to next page
                        onPageChangedListener?.onPageChanged()
                    }
                }
            }

            return@onCreateView root
        }
    }

    companion object {
        private const val TOP_TEXT: String = "TOP_TEXT"
        private const val CENTER_DRAWABLE: String = "CENTER_DRAWABLE"
        private const val BOTTOM_TEXT: String = "BOTTOM_TEXT"
        private const val EXTRA_TEXT: String = "EXTRA_TEXT"
        private const val LAST_FRAGMENT: String = "LAST_FRAGMENT"

        @JvmStatic
        fun newInstance(
            topText: String,
            centerDrawable: Drawable,
            bottomText: String,
            lastFragment: Boolean = false,
            extraText: String? = null
        ) =
            OnboardingFragment().apply {
                arguments = Bundle().apply {
                    putString(TOP_TEXT, topText)
                    val drawableBm = centerDrawable.toBitmap()

                    putByteArray(CENTER_DRAWABLE, ByteArrayOutputStream().run {
                        drawableBm.compress(Bitmap.CompressFormat.JPEG, 100, this)
                        toByteArray()
                    })

                    putString(BOTTOM_TEXT, bottomText)
                    putBoolean(LAST_FRAGMENT, lastFragment)
                    putString(EXTRA_TEXT, extraText)
                }
            }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is OnPageContinueListener) {
            onPageChangedListener = context
        } else {
            throw ClassCastException(context.toString()
                    + " must implement OnMessageOptionSelected")
        }
    }
}