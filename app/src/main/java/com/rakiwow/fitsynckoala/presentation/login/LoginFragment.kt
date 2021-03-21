package com.rakiwow.fitsynckoala.presentation.login

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.rakiwow.fitsynckoala.BuildConfig
import com.rakiwow.fitsynckoala.R
import com.rakiwow.fitsynckoala.databinding.FragmentLoginBinding
import com.rakiwow.fitsynckoala.model.ExchangeTokenResult
import com.rakiwow.fitsynckoala.util.DataState
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class LoginFragment : Fragment() {

    // TODO move business logic to view model
    private val loginViewModel: LoginViewModel by viewModels()

    private val navArgs: LoginFragmentArgs by navArgs()

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val STRAVA_CLIENT_ID = BuildConfig.STRAVA_CLIENT_ID
        private const val STRAVA_CLIENT_SECRET = BuildConfig.STRAVA_CLIENT_SECRET
        private const val REDIRECT_URI = "http://open.fitkoala"
        private const val ACCESS_DENIED_URI = "http://open.fitkoala?state=&error=access_denied"
        private const val HOST = "open.fitkoala"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupView()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun setupView() {
        val uri = Uri.parse("https://www.strava.com/oauth/mobile/authorize")
            .buildUpon()
            .appendQueryParameter("client_id", STRAVA_CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("approval_prompt", "auto")
            .appendQueryParameter("scope", "activity:write,read")
            .build()

        binding.wvAuthorize.settings.javaScriptEnabled = true
        binding.wvAuthorize.settings.domStorageEnabled = true
        binding.wvAuthorize.loadUrl(uri.toString())

        binding.wvAuthorize.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView,
                url: String
            ): Boolean {
                Timber.d(url)
                val code = Uri.parse(url).getQueryParameter("code")
                val error = Uri.parse(url).getQueryParameter("error")
                val host = Uri.parse(url).host
                // If the return code and host has a valid value - request exchange token
                if (!code.isNullOrEmpty()) {
                    if (HOST == host) {
                        binding.wvAuthorize.visibility = View.GONE
                        loginViewModel.fetchExchangeTokenResult(
                            STRAVA_CLIENT_ID.toInt(),
                            STRAVA_CLIENT_SECRET,
                            code
                        )
                        return false
                    }
                } else if (!error.isNullOrEmpty()) {
                    // Auth failed
                    if (url == ACCESS_DENIED_URI) {
                        Toast.makeText(
                            context,
                            getString(R.string.access_denied_rationale),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            getString(R.string.auth_failed_try_again),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return false
                }
                view.loadUrl(url)
                return false
            }
        }
    }

    private fun observeViewModel() {
        loginViewModel.exchangeTokenResult.observe(viewLifecycleOwner, { event ->
            event?.getContentIfNotHandled()?.let { dataState ->
                when (dataState) {
                    is DataState.Success<ExchangeTokenResult> -> {
                        binding.pbLogin.visibility = View.GONE
                        binding.vProgressBackground.visibility = View.GONE
                        with(dataState.data) {
                            loginViewModel.persistInstance(
                                accessToken = accessToken,
                                expiresAt = expiresAt,
                                expiresIn = expiresIn,
                                refreshToken = refreshToken
                            )
                        }
                        findNavController().navigate(
                            LoginFragmentDirections.actionLoginFragmentToSyncFragment(
                                navArgs.distance,
                                navArgs.elapsedTime,
                                navArgs.kcal
                            )
                        )
                    }
                    is DataState.Loading -> {
                        binding.pbLogin.visibility = View.VISIBLE
                        binding.vProgressBackground.visibility = View.VISIBLE
                    }
                    is DataState.Error -> {
                        binding.pbLogin.visibility = View.GONE
                        binding.vProgressBackground.visibility = View.GONE
                        Toast.makeText(
                            context,
                            getString(R.string.network_error_try_again),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        })

        // Observe datastore data, or else values are null
        loginViewModel.accessTokenFlow.observe(viewLifecycleOwner, {})
        loginViewModel.expiresAtFlow.observe(viewLifecycleOwner, {})
        loginViewModel.expiresInFlow.observe(viewLifecycleOwner, {})
        loginViewModel.refreshTokenFlow.observe(viewLifecycleOwner, {})
    }
}