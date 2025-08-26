import SwiftUI
import SharedApp

struct ContentView: View {
    
    var body: some View {
        NavigationView {
            Text("Dari - Smart Finance Tracker")
                .font(.title)
                .padding()
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
}

#Preview {
    ContentView()
}