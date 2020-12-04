package com.example.hobbyfi.ui.base

interface TextFieldInputValidationOnus {
    fun initTextFieldValidators()

    fun assertTextFieldsInvalidity(): Boolean
}