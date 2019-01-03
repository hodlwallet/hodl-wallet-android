package co.hodlwallet.presenter.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

import co.hodlwallet.presenter.customviews.BRButton;
import co.hodlwallet.R;
import co.hodlwallet.tools.animation.BRAnimator;

/**
 * BreadWallet
 * <p>
 * Created by Igor Guerrero <igor@bitstop.co> on 1/1/19.
 * Copyright (c) 2019 HODL Wallet
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

public class FragmentSegwit extends Fragment {
    private static final String TAG = FragmentSegwit.class.getName();

    private BRButton ok;
    private ConstraintLayout mainLayout;
    private RelativeLayout background;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_segwit, container, false);
        ok = (BRButton) rootView.findViewById(R.id.ok);
        mainLayout = (ConstraintLayout) rootView.findViewById(R.id.signal_layout);
        background = (RelativeLayout) rootView.findViewById(R.id.layout);

        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ViewTreeObserver observer = mainLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                observer.removeOnGlobalLayoutListener(this);
                BRAnimator.animateBackgroundDim(background, false);
                BRAnimator.animateSignalSlide(mainLayout, false, null);
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        BRAnimator.animateBackgroundDim(background, true);
        BRAnimator.animateSignalSlide(mainLayout, true, new BRAnimator.OnSlideAnimationEnd() {
            @Override
            public void onAnimationEnd() {
                if (getActivity() != null)
                    getActivity().getFragmentManager().popBackStack();
            }
        });

    }

    @Override
    public void onResume() { super.onResume(); }

    @Override
    public void onPause() {
        super.onPause();
    }

}