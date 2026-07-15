//
//  DismissKeyboardOnTap.swift
//  Copilot
//
//  Tap-anywhere keyboard dismissal for forms and lists. Implemented as a
//  UIKit tap recognizer on the window with cancelsTouchesInView = false,
//  so it never swallows touches: buttons, toggles, and rows keep working
//  exactly as before, and the tap merely closes the keyboard alongside.
//  Taps inside a text field are ignored so moving the cursor or hopping
//  between fields doesn't fight with focus.
//

import SwiftUI
import UIKit

extension View {
    /// Closes the keyboard when the user taps anywhere outside a text field.
    func dismissKeyboardOnTap() -> some View {
        background(KeyboardDismissInstaller())
    }
}

/// Marker subclass (and its own delegate) so each window installs the
/// recognizer at most once, no matter how many screens use the modifier.
private final class KeyboardDismissTapGesture: UITapGestureRecognizer, UIGestureRecognizerDelegate {
    /// Run alongside every other gesture instead of competing with them.
    func gestureRecognizer(
        _ gestureRecognizer: UIGestureRecognizer,
        shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer
    ) -> Bool {
        true
    }

    /// Skip touches that land inside a text input, where a tap means
    /// "place the cursor" or "focus this field", not "dismiss".
    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldReceive touch: UITouch) -> Bool {
        var view = touch.view
        while let current = view {
            if current is UITextField || current is UITextView { return false }
            view = current.superview
        }
        return true
    }
}

/// Invisible helper view that installs the recognizer on its window.
private struct KeyboardDismissInstaller: UIViewRepresentable {
    func makeUIView(context: Context) -> InstallerView {
        InstallerView()
    }

    func updateUIView(_ uiView: InstallerView, context: Context) {}

    final class InstallerView: UIView {
        override init(frame: CGRect) {
            super.init(frame: frame)
            isUserInteractionEnabled = false
        }

        required init?(coder: NSCoder) {
            fatalError("init(coder:) has not been implemented")
        }

        override func didMoveToWindow() {
            super.didMoveToWindow()
            guard let window,
                  !(window.gestureRecognizers ?? []).contains(where: { $0 is KeyboardDismissTapGesture })
            else { return }

            // Sending endEditing to the window resigns whichever text
            // field currently holds the keyboard.
            let tap = KeyboardDismissTapGesture(target: window, action: #selector(UIView.endEditing(_:)))
            tap.cancelsTouchesInView = false
            tap.requiresExclusiveTouchType = false
            tap.delegate = tap
            window.addGestureRecognizer(tap)
        }
    }
}
