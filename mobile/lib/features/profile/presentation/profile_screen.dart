import 'package:flutter/material.dart';

import '../../../app/app_scope.dart';
import '../../../core/network/api_exception.dart';
import '../data/data_export.dart';
import 'profile_forms.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  bool _exporting = false;

  /// [tileContext] positions the share popover on iPad.
  Future<void> _exportData(BuildContext tileContext) async {
    final deps = AppScope.read(context);
    final messenger = ScaffoldMessenger.of(context);
    final box = tileContext.findRenderObject() as RenderBox?;
    final origin = box == null ? null : box.localToGlobal(Offset.zero) & box.size;

    setState(() => _exporting = true);
    try {
      await DataExporter(deps.api, deps.shareFile).export(origin: origin);
    } on ApiException catch (e) {
      messenger.showSnackBar(SnackBar(content: Text(e.message)));
    } catch (_) {
      messenger.showSnackBar(const SnackBar(content: Text('Dosya paylaşılamadı.')));
    } finally {
      if (mounted) setState(() => _exporting = false);
    }
  }

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
          Builder(
            builder: (tileContext) => ListTile(
              leading: const Icon(Icons.download_outlined),
              title: const Text('Verilerimi indir'),
              subtitle: const Text('Tüm kayıtların, JSON dosyası olarak'),
              enabled: !_exporting,
              trailing: _exporting
                  ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2))
                  : null,
              onTap: () => _exportData(tileContext),
            ),
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
