package co.hodlwallet.presenter.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import co.hodlwallet.HodlApp;
import co.hodlwallet.R;
import co.hodlwallet.presenter.activities.settings.WebViewActivity;
import co.hodlwallet.presenter.customviews.BRButton;
import co.hodlwallet.presenter.customviews.BRKeyboard;
import co.hodlwallet.presenter.customviews.BRLinearLayoutWithCaret;
import co.hodlwallet.tools.animation.BRAnimator;
import co.hodlwallet.tools.animation.SlideDetector;
import co.hodlwallet.tools.manager.BRClipboardManager;
import co.hodlwallet.tools.manager.BRSharedPrefs;
import co.hodlwallet.tools.qrcode.QRUtils;
import co.hodlwallet.tools.threads.BRExecutor;
import co.hodlwallet.tools.util.BRConstants;
import co.hodlwallet.tools.util.Utils;
import co.hodlwallet.wallet.BRWalletManager;

import static co.hodlwallet.tools.animation.BRAnimator.animateBackgroundDim;
import static co.hodlwallet.tools.animation.BRAnimator.animateSignalSlide;
import static co.platform.HTTPServer.URL_SUPPORT;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/29/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class FragmentLegacyAddress extends Fragment {
    private static final String TAG = FragmentLegacyAddress.class.getName();

    public TextView mTitle;
    public TextView mAddress;
    public ImageView mQrImage;
    public LinearLayout backgroundLayout;
    public LinearLayout signalLayout;
    private String legacyAddress;
    private View separator;
    private BRButton shareButton;
    private Button requestButton;
    private BRLinearLayoutWithCaret copiedLayout;
    private boolean isLegacy;
    private ImageButton close;
    private Handler copyCloseHandler = new Handler();
    private BRKeyboard keyboard;
    private View separator2;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.

        View rootView = inflater.inflate(R.layout.fragment_receive, container, false);
        mTitle = (TextView) rootView.findViewById(R.id.title);
        mAddress = (TextView) rootView.findViewById(R.id.address_text);
        mQrImage = (ImageView) rootView.findViewById(R.id.qr_image);
        backgroundLayout = (LinearLayout) rootView.findViewById(R.id.background_layout);
        signalLayout = (LinearLayout) rootView.findViewById(R.id.signal_layout);
        shareButton = (BRButton) rootView.findViewById(R.id.share_button);
        copiedLayout = (BRLinearLayoutWithCaret) rootView.findViewById(R.id.copied_layout);
        requestButton = (Button) rootView.findViewById(R.id.request_button);
        keyboard = (BRKeyboard) rootView.findViewById(R.id.keyboard);
        keyboard.setBRButtonBackgroundResId(R.drawable.keyboard_white_button);
        keyboard.setBRKeyboardColor(R.color.white);
        separator = rootView.findViewById(R.id.separator);
        close = (ImageButton) rootView.findViewById(R.id.close_button);
        separator2 = rootView.findViewById(R.id.separator2);
        separator2.setVisibility(View.GONE);

        mTitle.setText(R.string.Settings_legacyAddressTitle);

        setListeners();
        BRWalletManager.getInstance().addBalanceChangedListener(new BRWalletManager.OnBalanceChanged() {
            @Override
            public void onBalanceChanged(long balance) {
                updateQr();
            }
        });

        ImageButton faq = (ImageButton) rootView.findViewById(R.id.faq_button);

        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                Activity app = getActivity();
                if (app == null) {
                    Log.e(TAG, "onClick: app is null, can't start the webview with url: " + URL_SUPPORT);
                    return;
                }

                BRAnimator.showSupportFragment(app, BRConstants.receive);
            }
        });

        signalLayout.removeView(copiedLayout);

        copiedLayout.setBackgroundColor(getContext().getColor(R.color.gray_background));

        signalLayout.setLayoutTransition(BRAnimator.getDefaultTransition());

        return rootView;
    }


    private void setListeners() {
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                String bitcoinUri = Utils.createBitcoinUrl(legacyAddress, 0, null, null, null);
                QRUtils.share(getActivity(), bitcoinUri);
            }
        });
        mAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                copyText();
            }
        });
        requestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                Activity app = getActivity();
                app.onBackPressed();
                BRAnimator.showRequestFragment(app, legacyAddress);
            }
        });

        backgroundLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                getActivity().onBackPressed();
            }
        });
        mQrImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                copyText();
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity app = getActivity();
                if (app != null)
                    app.getFragmentManager().popBackStack();
            }
        });
    }

    private void showCopiedLayout(boolean b) {
        if (!b) {
            signalLayout.removeView(copiedLayout);
            copyCloseHandler.removeCallbacksAndMessages(null);
        } else {
            if (signalLayout.indexOfChild(copiedLayout) == -1) {
                signalLayout.addView(copiedLayout, signalLayout.indexOfChild(shareButton));
                copyCloseHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        signalLayout.removeView(copiedLayout);
                    }
                }, 2000);
            } else {
                copyCloseHandler.removeCallbacksAndMessages(null);
                signalLayout.removeView(copiedLayout);
            }
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ViewTreeObserver observer = signalLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                observer.removeGlobalOnLayoutListener(this);
                animateBackgroundDim(backgroundLayout, false);
                animateSignalSlide(signalLayout, false, null);
            }
        });

        Bundle extras = getArguments();
        isLegacy = extras.getBoolean("legacy");
        if (!isLegacy) {
            signalLayout.removeView(separator);
            signalLayout.removeView(requestButton);
            mTitle.setText(getString(R.string.UnlockScreen_myAddress));
        }

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                updateQr();
            }
        });

    }

    private void updateQr() {
        final Context ctx = getContext() == null ? HodlApp.getBreadContext() : (Activity) getContext();
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                boolean success = BRWalletManager.refreshAddress(ctx);
                if (!success) {
                    if (ctx instanceof Activity) {
                        BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                            @Override
                            public void run() {
                                ((Activity) ctx).onBackPressed();
                            }
                        });

                    }
                    return;
                }
                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        // TODO fix this, use legacy instead of normal address
                        legacyAddress = BRSharedPrefs.getLegacyAddress(ctx);
                        mAddress.setText(legacyAddress);
                        boolean generated = QRUtils.generateQR(ctx, "bitcoin:" + legacyAddress, mQrImage);
                        if (!generated)
                            throw new RuntimeException("failed to generate qr image for address");
                    }
                });
            }
        });

    }

    private void copyText() {
        BRClipboardManager.putClipboard(getContext(), mAddress.getText().toString());
        showCopiedLayout(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        animateBackgroundDim(backgroundLayout, true);
        animateSignalSlide(signalLayout, true, new BRAnimator.OnSlideAnimationEnd() {
            @Override
            public void onAnimationEnd() {
                if (getActivity() != null)
                    getActivity().getFragmentManager().popBackStack();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

}
