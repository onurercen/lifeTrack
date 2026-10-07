import 'package:flutter/material.dart';

import '../../../app/app_scope.dart';
import '../../../core/widgets/entity_form_screen.dart';

/// Changes the display name. Pops `true` after saving.
class EditNameScreen extends StatefulWidget {
  const EditNameScreen({super.key, required this.initialName});

  final String initialName;

  @override
  State<EditNameScreen> createState() => _EditNameScreenState();
}

class _EditNameScreenState extends State<EditNameScreen> {
  late final TextEditingController _name = TextEditingController(text: widget.initialName);

  @override
  void dispose() {
    _name.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return EntityFormScreen(
      title: 'Adını değiştir',
      submitLabel: 'Kaydet',
      onSubmit: () => AppScope.read(context).auth.updateName(_name.text.trim()),
      fieldsBuilder: (context, serverErrors) => [
        FormTextField(
          controller: _name,
          label: 'Ad soyad',
          requiredMessage: 'Ad soyad zorunludur',
          icon: Icons.person_outline,
          maxLength: 255,
          serverError: serverErrors['name'],
        ),
      ],
    );
  }
}

/// Changes the password; other devices are signed out. Pops `true` after saving.
class ChangePasswordScreen extends StatefulWidget {
  const ChangePasswordScreen({super.key});

  @override
  State<ChangePasswordScreen> createState() => _ChangePasswordScreenState();
}

class _ChangePasswordScreenState extends State<ChangePasswordScreen> {
  final _current = TextEditingController();
  final _new = TextEditingController();
  final _confirmation = TextEditingController();

  @override
  void dispose() {
    _current.dispose();
    _new.dispose();
    _confirmation.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return EntityFormScreen(
      title: 'Şifreyi değiştir',
      submitLabel: 'Şifreyi değiştir',
      onSubmit: () => AppScope.read(context).auth.changePassword(
            currentPassword: _current.text,
            newPassword: _new.text,
          ),
      fieldsBuilder: (context, serverErrors) => [
        FormTextField(
          controller: _current,
          label: 'Mevcut şifre',
          requiredMessage: 'Mevcut şifre zorunludur',
          icon: Icons.lock_outline,
          obscureText: true,
          serverError: serverErrors['currentPassword'],
        ),
        FormTextField(
          controller: _new,
          label: 'Yeni şifre',
          requiredMessage: 'Yeni şifre zorunludur',
          icon: Icons.lock_reset,
          obscureText: true,
          serverError: serverErrors['newPassword'],
          validator: (text) => text.length < 6 ? 'Şifre en az 6 karakter olmalıdır' : null,
        ),
        FormTextField(
          controller: _confirmation,
          label: 'Yeni şifre (tekrar)',
          requiredMessage: 'Yeni şifreyi tekrar giriniz',
          icon: Icons.lock_reset,
          obscureText: true,
          validator: (text) => text != _new.text ? 'Şifreler eşleşmiyor' : null,
        ),
        Text(
          'Şifren değişince diğer cihazlardaki oturumların kapanır.',
          style: Theme.of(context).textTheme.bodySmall,
        ),
      ],
    );
  }
}

/// Permanently deletes the account after asking for the password.
class DeleteAccountScreen extends StatefulWidget {
  const DeleteAccountScreen({super.key});

  @override
  State<DeleteAccountScreen> createState() => _DeleteAccountScreenState();
}

class _DeleteAccountScreenState extends State<DeleteAccountScreen> {
  final _password = TextEditingController();

  @override
  void dispose() {
    _password.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return EntityFormScreen(
      title: 'Hesabı sil',
      submitLabel: 'Hesabımı kalıcı olarak sil',
      onSubmit: () => AppScope.read(context).auth.deleteAccount(password: _password.text),
      fieldsBuilder: (context, serverErrors) => [
        Card(
          color: theme.colorScheme.errorContainer,
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Text(
              'Hesabın ve tüm koşu, kitap ve medya kayıtların kalıcı olarak silinir. Bu işlem geri alınamaz.',
              style: TextStyle(color: theme.colorScheme.onErrorContainer),
            ),
          ),
        ),
        FormTextField(
          controller: _password,
          label: 'Şifre',
          requiredMessage: 'Onaylamak için şifreni gir',
          icon: Icons.lock_outline,
          obscureText: true,
          serverError: serverErrors['password'],
        ),
      ],
    );
  }
}
