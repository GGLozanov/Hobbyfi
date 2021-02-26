package com.example.hobbyfi.adapters.onboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.hobbyfi.R
import com.example.hobbyfi.shared.convertDrawableResToBitmap
import com.example.hobbyfi.ui.onboard.OnboardingFragment

class OnboardingViewPagerAdapter(
    private val context: Context,
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {
    companion object {
        const val PAGE_COUNT = 3
    }

    override fun getItemCount(): Int = PAGE_COUNT

    override fun createFragment(position: Int): Fragment {
        val lastFragment = position == (PAGE_COUNT - 1)
        return when(position) {
            0 -> OnboardingFragment.newInstance(
                context.getString(R.string.welcome_to_app),
                context.convertDrawableResToBitmap(R.drawable.hobbyfi_icon, 200, 200),
                context.getString(R.string.app_description),
                lastFragment
            )
            1 -> OnboardingFragment.newInstance(
                context.getString(R.string.auth_explanation),
                context.convertDrawableResToBitmap(R.drawable.ic_baseline_security_white_24, 200, 200),
                context.getString(R.string.facebook_mention),
                lastFragment
            )
            2 -> OnboardingFragment.newInstance(
                context.getString(R.string.chatrooms_explanation),
                context.convertDrawableResToBitmap(R.drawable.chatroom_default_pic, 200, 200),
                context.getString(R.string.app_description_outro),
                lastFragment,
                context.getString(R.string.closing_onboarding)
            )
            else -> throw IllegalStateException("Invalid position for ViewPager!")
        }
    }
}