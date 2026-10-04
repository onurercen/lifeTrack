import 'package:flutter/material.dart';

import '../../books/presentation/books_screen.dart';
import '../../media/presentation/media_screen.dart';
import '../../profile/presentation/profile_screen.dart';
import '../../running/presentation/running_screen.dart';
import 'dashboard_screen.dart';

class HomeShell extends StatefulWidget {
  const HomeShell({super.key});

  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> {
  int _index = 0;
  final _dashboardKey = GlobalKey<DashboardScreenState>();

  late final _tabs = <Widget>[
    DashboardScreen(key: _dashboardKey),
    const RunningScreen(),
    const BooksScreen(),
    const MediaScreen(),
    const ProfileScreen(),
  ];

  void _select(int index) {
    // Other tabs may have changed data since the dashboard was last loaded.
    if (index == 0 && _index != 0) _dashboardKey.currentState?.reload();
    setState(() => _index = index);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(index: _index, children: _tabs),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: _select,
        destinations: const [
          NavigationDestination(icon: Icon(Icons.home_outlined), selectedIcon: Icon(Icons.home), label: 'Ana Sayfa'),
          NavigationDestination(icon: Icon(Icons.directions_run_outlined), selectedIcon: Icon(Icons.directions_run), label: 'Koşu'),
          NavigationDestination(icon: Icon(Icons.menu_book_outlined), selectedIcon: Icon(Icons.menu_book), label: 'Kitap'),
          NavigationDestination(icon: Icon(Icons.movie_outlined), selectedIcon: Icon(Icons.movie), label: 'Medya'),
          NavigationDestination(icon: Icon(Icons.person_outline), selectedIcon: Icon(Icons.person), label: 'Profil'),
        ],
      ),
    );
  }
}
