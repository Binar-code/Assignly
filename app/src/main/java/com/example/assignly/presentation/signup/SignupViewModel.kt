package com.example.assignly.presentation.signup

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.health.connect.datatypes.HeartRateRecord
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.assignly.api.NetworkService
import com.example.assignly.presentation.login.LoginUiState
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.ByteArrayOutputStream

sealed class SignupUiState {
    data class Idle (
        val login: String = "",
        val tag: String = "",
        val password: String = "",
        val passwordRepeat: String = "",
        val image: Uri? = null
    ): SignupUiState()

    data class Loading (
        val login: String,
        val tag: String,
        val password: String,
        val passwordRepeat: String,
        val image: Uri?
    ): SignupUiState()

    data class Error (
        val login: String,
        val tag: String,
        val password: String,
        val passwordRepeat: String,
        val image: Uri?,
        val errorMessage: String
    ): SignupUiState()

    data class Success (
        val successMessage: String
    ): SignupUiState()
}

class SignupViewModel(application: Application): AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<SignupUiState>(SignupUiState.Idle())
    val uiState = _uiState.asStateFlow()

    fun loginChange(newLogin: String) {
        when (val current = _uiState.value) {
            is SignupUiState.Idle -> {
                _uiState.value = current.copy(login = newLogin)
            }

            is SignupUiState.Error -> {
                _uiState.value = SignupUiState.Idle(
                    login = current.login,
                    tag = current.tag,
                    password = current.password,
                    passwordRepeat = current.passwordRepeat,
                    image = current.image
                )
            }

            else -> Unit
        }
    }

    fun tagChange(newTag: String) {
        when (val current = _uiState.value) {
            is SignupUiState.Idle -> {
                _uiState.value = current.copy(tag = newTag)
            }

            is SignupUiState.Error -> {
                _uiState.value = SignupUiState.Idle(
                    login = current.login,
                    tag = current.tag,
                    password = current.password,
                    passwordRepeat = current.passwordRepeat,
                    image = current.image
                )
            }

            else -> Unit
        }
    }

    fun passwordChange(newPassword: String) {
        when (val current = _uiState.value) {
            is SignupUiState.Idle -> {
                _uiState.value = current.copy(password = newPassword)
            }

            is SignupUiState.Error -> {
                _uiState.value = SignupUiState.Idle(
                    login = current.login,
                    tag = current.tag,
                    password = current.password,
                    passwordRepeat = current.passwordRepeat,
                    image = current.image
                )
            }

            else -> Unit
        }
    }

    fun passwordRepeatChange(newPasswordRepeat: String) {
        when (val current = _uiState.value) {
            is SignupUiState.Idle -> {
                _uiState.value = current.copy(passwordRepeat = newPasswordRepeat)
            }

            is SignupUiState.Error -> {
                _uiState.value = SignupUiState.Idle(
                    login = current.login,
                    tag = current.tag,
                    password = current.password,
                    passwordRepeat = current.passwordRepeat,
                    image = current.image
                )
            }

            else -> Unit
        }
    }

    fun imageChange(newImage: Uri?) {
        when (val current = _uiState.value) {
            is SignupUiState.Idle -> {
                _uiState.value = current.copy(image = newImage)
            }

            is SignupUiState.Error -> {
                _uiState.value = SignupUiState.Idle(
                    login = current.login,
                    tag = current.tag,
                    password = current.password,
                    passwordRepeat = current.passwordRepeat,
                    image = current.image
                )
            }

            else -> Unit
        }
    }

    fun signup() {
        val current = _uiState.value

        if (current is SignupUiState.Idle) {
            if (current.login.isBlank() || current.password.isBlank()) {
                _uiState.value = SignupUiState.Error(
                    login = current.login,
                    tag = current.tag,
                    password = current.password,
                    passwordRepeat = current.passwordRepeat,
                    image = current.image,
                    errorMessage = "Fields shouldn't be blank"
                )
                return
            }

            _uiState.value = SignupUiState.Loading(
                login = current.login,
                tag = current.tag,
                password = current.password,
                passwordRepeat = current.passwordRepeat,
                image = current.image
            )

            viewModelScope.launch {
                try {
                    if (current.password != current.passwordRepeat)  {
                        _uiState.value = SignupUiState.Error (
                            login = current.login,
                            tag = current.tag,
                            password = current.password,
                            passwordRepeat = current.passwordRepeat,
                            image = current.image,
                            errorMessage = "passwords don't match"
                        )
                    } else {
                        val request = NetworkService.signup.signup(
                            login = current.login,
                            tag = current.tag,
                            password = current.password,
                            image = imageConvert(current.image)
                        )
                    }
                } catch (e: HttpException) {
                    if (e.code() == 409) {
                        _uiState.value = SignupUiState.Error (
                            login = current.login,
                            tag = current.tag,
                            password = current.password,
                            passwordRepeat = current.passwordRepeat,
                            image = current.image,
                            errorMessage = "user already exists"
                        )
                    } else if (e.code() == 404) {
                        _uiState.value = SignupUiState.Error (
                            login = current.login,
                            tag = current.tag,
                            password = current.password,
                            passwordRepeat = current.passwordRepeat,
                            image = current.image,
                            errorMessage = "could not add user"
                        )
                    }
                }
            }
        }
    }

    private fun imageConvert(uri: Uri?): String {
        if (uri == null) return ""
        return try {
            val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
            inputStream.use {
                val bitmap = BitmapFactory.decodeStream(it) ?: return ""
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()
                Base64.encodeToString(byteArray, Base64.DEFAULT)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }
}