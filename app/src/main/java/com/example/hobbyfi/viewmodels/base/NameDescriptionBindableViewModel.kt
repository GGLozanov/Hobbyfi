package com.example.hobbyfi.viewmodels.base

import androidx.lifecycle.LiveData
import com.example.hobbyfi.shared.PredicateMutableLiveData
import com.example.hobbyfi.shared.equalsOrBiggerThan
import com.example.hobbyfi.shared.validateBy

class NameDescriptionBindableViewModel : NameDescriptionBindable, TwoWayDataBindable by TwoWayDataBindableViewModel() {
    override val name: PredicateMutableLiveData<String> = PredicateMutableLiveData { it == null || it.isEmpty() || it.length >= 25 }
    override val description: PredicateMutableLiveData<String?> = PredicateMutableLiveData { it != null &&
            it.length >= 30 }

    override val combinedObserversInvalidity: LiveData<Boolean> get() = validateBy(
        name.invalidity,
        description.invalidity
    )
}