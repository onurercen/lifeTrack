import 'package:flutter/material.dart';

import '../../../app/app_scope.dart';

class ProfileScreen extends StatelessWidget {
  const ProfileScreen({super.key});

  Future<void> _confirmLogout(BuildContext context) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Çıkış yap'),
        content: const Text('Oturumu kapatmak istediğine emin misin?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Vazgeç')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Çıkış yap')),
        ],
      ),
    );
    if (confirmed == true && context.mounted) {
      await AppScope.read(context).auth.logout();
    }
  }

  @override
  Widget build(BuildContext context) {
    final user = AppScope.of(context).auth.user;

    return Scaffold(
      appBar: AppBar(title: const Text('Profil')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          ListTile(
            leading: const CircleAvatar(child: Icon(Icons.person)),
            title: Text(user?.name ?? '—'),
            subtitle: Text(user?.email ?? 'Profil bilgisi yüklenemedi'),
          ),
          const Divider(height: 32),
          ListTile(
            leading: Icon(Icons.logout, color: Theme.of(context).colorScheme.error),
            title: Text('Çıkış yap', style: TextStyle(color: Theme.of(context).colorScheme.error)),
            onTap: () => _confirmLogout(context),
          ),
        ],
      ),
    );
  }
}
