import 'package:flutter/material.dart';

import '../../../app/app_scope.dart';
import 'profile_forms.dart';

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

  Future<void> _open(BuildContext context, Widget screen, String savedMessage) async {
    final messenger = ScaffoldMessenger.of(context);
    final saved = await Navigator.of(context).push<bool>(MaterialPageRoute(builder: (_) => screen));
    if (saved == true) messenger.showSnackBar(SnackBar(content: Text(savedMessage)));
  }

  @override
  Widget build(BuildContext context) {
    final user = AppScope.of(context).auth.user;
    final error = Theme.of(context).colorScheme.error;

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
            leading: const Icon(Icons.edit_outlined),
            title: const Text('Adını değiştir'),
            enabled: user != null,
            onTap: () => _open(context, EditNameScreen(initialName: user!.name), 'Adın güncellendi'),
          ),
          ListTile(
            leading: const Icon(Icons.lock_reset),
            title: const Text('Şifreyi değiştir'),
            onTap: () => _open(context, const ChangePasswordScreen(), 'Şifren değiştirildi'),
          ),
          const Divider(height: 32),
          ListTile(
            leading: Icon(Icons.logout, color: error),
            title: Text('Çıkış yap', style: TextStyle(color: error)),
            onTap: () => _confirmLogout(context),
          ),
          ListTile(
            leading: Icon(Icons.delete_forever_outlined, color: error),
            title: Text('Hesabı sil', style: TextStyle(color: error)),
            onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const DeleteAccountScreen())),
          ),
        ],
      ),
    );
  }
}
