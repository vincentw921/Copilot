//
//  HomeController.swift
//  Copilot
//
//  Created by Vincent Wang on 9/14/25.
//

import SwiftUI

final class HomeController: UIHostingController<HomeView> {
    init() {
        super.init(rootView: HomeView())
        title = "Home"
    }
    @MainActor required dynamic init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder, rootView: HomeView())
    }
}

