import SwiftUI
import SharedApp

@main
struct iOSApp: App {
    
    init() {
        // Initialize Koin for iOS
        KoinInitHelper().initialize()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}