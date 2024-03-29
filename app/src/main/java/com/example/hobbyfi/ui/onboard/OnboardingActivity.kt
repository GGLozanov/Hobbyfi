package com.example.hobbyfi.ui.onboard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.hobbyfi.adapters.onboard.OnboardingViewPagerAdapter
import com.example.hobbyfi.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity(), OnboardingFragment.OnPageContinueListener {

    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.adapter = OnboardingViewPagerAdapter(this, supportFragmentManager, lifecycle)
    }
    override fun onBackPressed() {
        with(binding) {
            if (viewPager.currentItem == 0) {
                super.onBackPressed()
            } else {
                viewPager.currentItem = viewPager.currentItem - 1
            }
        }
    }

    override fun onPageChanged() {
        binding.viewPager.currentItem++
    }
}