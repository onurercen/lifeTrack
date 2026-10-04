import 'package:flutter/material.dart';
import '../../../app/app_scope.dart';
import '../../../core/network/api_exception.dart';

class RegisterScreen extends StatefulWidget {
	const RegisterScreen({super.key});

	@override
	State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
	final _formKey = GlobalKey<FormState>();
	final _nameController = TextEditingController();
	final _emailController = TextEditingController();
	final _passwordController = TextEditingController();
	final _confirmPasswordController = TextEditingController();
	bool _obscurePassword = true;
	bool _obscureConfirmation = true;
	bool _isSubmitting = false;

	@override
	void dispose() {
		_nameController.dispose();
		_emailController.dispose();
		_passwordController.dispose();
		_confirmPasswordController.dispose();
		super.dispose();
	}

	Future<void> _submit() async {
		if (!_formKey.currentState!.validate()) return;

		setState(() => _isSubmitting = true);
		try {
			await AppScope.read(context).auth.register(
				name: _nameController.text.trim(),
				email: _emailController.text.trim(),
				password: _passwordController.text,
			);
			// AuthGate switches to the home screen once the session starts.
		} on ApiException catch (error) {
			if (!mounted) return;
			ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.message)));
		} catch (_) {
			if (!mounted) return;
			ScaffoldMessenger.of(context).showSnackBar(
				const SnackBar(content: Text('Sunucuya bağlanılamadı.')),
			);
		} finally {
			if (mounted) setState(() => _isSubmitting = false);
		}
	}

	@override
	Widget build(BuildContext context) {
		return Scaffold(
			appBar: AppBar(title: const Text('Kayıt ol')),
			body: SafeArea(
				child: Center(
					child: SingleChildScrollView(
						padding: const EdgeInsets.fromLTRB(24, 12, 24, 24),
						child: ConstrainedBox(
							constraints: const BoxConstraints(maxWidth: 420),
							child: Form(
								key: _formKey,
								child: Column(
									crossAxisAlignment: CrossAxisAlignment.stretch,
									children: [
										Text(
											'LifeTrack hesabını oluştur',
											style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.bold),
										),
										const SizedBox(height: 8),
										const Text('Takip etmek istediğin alanları tek bir yerde toplamaya başla.'),
										const SizedBox(height: 28),
										TextFormField(
											controller: _nameController,
											textInputAction: TextInputAction.next,
											autofillHints: const [AutofillHints.name],
											decoration: const InputDecoration(
												labelText: 'Ad soyad',
												prefixIcon: Icon(Icons.person_outline),
												border: OutlineInputBorder(),
											),
											validator: (value) => value == null || value.trim().isEmpty ? 'Ad soyad zorunludur' : null,
										),
										const SizedBox(height: 16),
										TextFormField(
											controller: _emailController,
											keyboardType: TextInputType.emailAddress,
											textInputAction: TextInputAction.next,
											autofillHints: const [AutofillHints.email],
											decoration: const InputDecoration(
												labelText: 'E-posta',
												prefixIcon: Icon(Icons.email_outlined),
												border: OutlineInputBorder(),
											),
											validator: _validateEmail,
										),
										const SizedBox(height: 16),
										TextFormField(
											controller: _passwordController,
											obscureText: _obscurePassword,
											textInputAction: TextInputAction.next,
											autofillHints: const [AutofillHints.newPassword],
											decoration: _passwordDecoration('Şifre', _obscurePassword, () => setState(() => _obscurePassword = !_obscurePassword)),
											validator: (value) {
												if (value == null || value.isEmpty) return 'Şifre zorunludur';
												if (value.length < 6) return 'Şifre en az 6 karakter olmalıdır';
												return null;
											},
										),
										const SizedBox(height: 16),
										TextFormField(
											controller: _confirmPasswordController,
											obscureText: _obscureConfirmation,
											textInputAction: TextInputAction.done,
											onFieldSubmitted: (_) => _submit(),
											decoration: _passwordDecoration('Şifre tekrarı', _obscureConfirmation, () => setState(() => _obscureConfirmation = !_obscureConfirmation)),
											validator: (value) {
												if (value == null || value.isEmpty) return 'Şifre tekrarı zorunludur';
												if (value != _passwordController.text) return 'Şifreler eşleşmiyor';
												return null;
											},
										),
										const SizedBox(height: 24),
										FilledButton(
											onPressed: _isSubmitting ? null : _submit,
											child: _isSubmitting
													? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2))
													: const Text('Kayıt ol'),
										),
										const SizedBox(height: 12),
										TextButton(
											onPressed: () => Navigator.pop(context),
											child: const Text('Zaten hesabın var mı? Giriş yap'),
										),
									],
								),
							),
						),
					),
				),
			),
		);
	}

	InputDecoration _passwordDecoration(String label, bool obscure, VoidCallback onToggle) {
		return InputDecoration(
			labelText: label,
			prefixIcon: const Icon(Icons.lock_outline),
			border: const OutlineInputBorder(),
			suffixIcon: IconButton(
				tooltip: obscure ? 'Şifreyi göster' : 'Şifreyi gizle',
				onPressed: onToggle,
				icon: Icon(obscure ? Icons.visibility_outlined : Icons.visibility_off_outlined),
			),
		);
	}

	String? _validateEmail(String? value) {
		if (value == null || value.trim().isEmpty) return 'E-posta zorunludur';
		if (!RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(value.trim())) return 'Geçerli bir e-posta giriniz';
		return null;
	}
}
