package com.example.hobbyfi.ui.chatroom

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.event.EventListAdapter
import com.example.hobbyfi.models.data.Event
import com.example.hobbyfi.shared.*
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.share.Sharer
import com.facebook.share.model.ShareLinkContent
import com.facebook.share.widget.ShareDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.prolificinteractive.materialcalendarview.CalendarDay
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.BranchError
import io.branch.referral.util.LinkProperties
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.kodein.di.generic.instance


@ExperimentalCoroutinesApi
class EventCalendarSelectionBottomSheetDialogFragment : EventSelectionBottomSheetDialogFragment() {
    override val eventsSource: LiveData<List<Event>>
        by lazy { activityViewModel.authEvents.map {
            it.filter { event -> (requireArguments()[Constants.CALENDAR_DAY] as CalendarDay) ==
                    event.calendarDayFromDate }
        } } // livedata filter {} go brrr
    private lateinit var navController: NavController // only need it here in all the bottomsheets

    private val callBackManager: CallbackManager by instance(tag = "callbackManager")

    override val eventListAdapter: EventListAdapter by lazy {
        EventListAdapter(
            eventsSource.value ?: arrayListOf(),
            { v: View, event: Event ->
                v.isEnabled = false

                navController.safeNavigate(
                    ChatroomMessageListFragmentDirections.actionChatroomMessageListFragmentToEventDetailsFragment(
                        event
                    )
                )
                dismiss()

                v.postDelayed({
                    v.isEnabled = true
                }, 1000) // event card tap antispam TODO: Fix
            }, { _: View, event: Event ->
                generateFacebookEventDeeplink(event)
            })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view.findViewById<MaterialButton>(R.id.delete_old_events_button).isVisible = false
        view.findViewById<MaterialTextView>(R.id.current_events_header).isVisible = false
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = findNavController()
    }

    private fun generateFacebookEventDeeplink(event: Event) {
        if(!connectivityManager.isConnected()) {
            context?.showFailureToast(
                getString(R.string.no_connection_error)
            )
            return
        }

        // create branch universal object to identify deeplink
        val buo = BranchUniversalObject()
            .setCanonicalIdentifier("event/${event.id}")
            .setTitle(event.name)
            .setContentDescription(event.description ?: "")
            .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
            .setLocalIndexMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)

        event.photoUrl?.let {
            buo.setContentImageUrl(it)
        }

        // deeplink creation
        val lp: LinkProperties = LinkProperties()
            .setChannel("facebook")
            .setFeature("sharing")
            .setStage("event share")
            .addControlParameter("\$og_title", "New event hitting ${activityViewModel.authChatroom.value!!.name}!")
            .addControlParameter(
                "\$og_description",
                "Check out the new event storming the ${activityViewModel.authChatroom.value!!.name} chatroom: ${event.name}! ${if(event.description != null)
                    "Described as, \"${event.description}\", you are surely not to miss this spectacle!" else ""}"
            )
            .addControlParameter(
                "\$og_app_id",
                getString(R.string.FB_APP_ID)
            )
            .addControlParameter(Constants.EVENT_ID, event.id.toString())
            .addControlParameter(Constants.CHATROOM_ID, event.chatroomId.toString())


        event.photoUrl?.let {
            lp.addControlParameter(
                "\$og_image_url",
                it
            ) // TODO: Somehow image content moderation and stuff not to get in trouble with Facebook, lul
        }

        buo.generateShortUrl(requireContext(), lp) { url: String, error: BranchError? ->
            if (error != null) {
                context?.showFailureToast(getString(R.string.deep_link_gen_fail))
                return@generateShortUrl
            }

            if (ShareDialog.canShow(ShareLinkContent::class.java)) {
                val shareDialog = ShareDialog(this)
                shareDialog.registerCallback(callBackManager, object : FacebookCallback<Sharer.Result> {
                    override fun onSuccess(result: Sharer.Result?) {
                        context?.showSuccessToast(getString(R.string.deep_link_success_share))
                    }

                    override fun onCancel() {
                        context?.showWarningToast(getString(R.string.deep_link_cancel_share))
                    }

                    override fun onError(error: FacebookException?) {
                        error?.printStackTrace()
                        Log.e("EventCalendarSBSDF", "Facebook share exception: ${error?.message}")
                        context?.showFailureToast(getString(R.string.deep_link_fail_share))
                    }
                })
                val linkContent = ShareLinkContent.Builder()
                    .setContentUrl(Uri.parse(url))
                    .build()
                Log.i("EventCalendarSBSDF", "Deep link gen: ${linkContent.contentUrl}")
                shareDialog.show(linkContent)
            } else {
                context?.showFailureToast(getString(R.string.share_dialog_fail))
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callBackManager.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        fun newInstance(calendarDay: CalendarDay): EventCalendarSelectionBottomSheetDialogFragment {
            val instance = EventCalendarSelectionBottomSheetDialogFragment()
            val args = Bundle()
            args.putParcelable(Constants.CALENDAR_DAY, calendarDay)
            instance.arguments = args

            return instance
        }
    }
}