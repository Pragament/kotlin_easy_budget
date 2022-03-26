/*
 *   Copyright 2021 Benoit LETONDOR
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.benoitletondor.easybudgetapp.view.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.databinding.FragmentOnboarding4Binding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Onboarding step 4 fragment
 *
 * @author Benoit LETONDOR
 */
@AndroidEntryPoint
class Onboarding4Fragment : OnboardingFragment<FragmentOnboarding4Binding>() {

    override val statusBarColor: Int
        get() = R.color.primary_dark

    override fun onCreateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentOnboarding4Binding = FragmentOnboarding4Binding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.onboardingScreen4NextButton?.setOnClickListener {
            done()
        }
    }
}
