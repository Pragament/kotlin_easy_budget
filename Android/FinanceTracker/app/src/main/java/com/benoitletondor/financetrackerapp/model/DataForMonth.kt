/*
 *   Copyright 2025 Benoit Letondor
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

package com.benoitletondor.FinanceTrackerapp.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import java.time.YearMonth

@Immutable
data class DataForMonth(
    val month: YearMonth,
    val daysData: Map<LocalDate, DataForDay>,
) {
    companion object {
        const val NUMBER_OF_LEEWAY_DAYS: Long = 6
    }
}

@Immutable
data class DataForDay(
    val day: LocalDate,
    val expenses: List<Expense>,
    val balance: Double,
    val checkedBalance: Double,
)
