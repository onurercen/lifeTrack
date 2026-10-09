import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// The six-digit code from a LifeTrack e-mail.
class CodeField extends StatelessWidget {
  const CodeField({super.key, required this.controller, this.serverError, this.onSubmitted});

  final TextEditingController controller;
  final String? serverError;
  final VoidCallback? onSubmitted;

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      controller: controller,
      keyboardType: TextInputType.number,
      textInputAction: onSubmitted == null ? TextInputAction.next : TextInputAction.done,
      autofillHints: const [AutofillHints.oneTimeCode],
      inputFormatters: [FilteringTextInputFormatter.digitsOnly, LengthLimitingTextInputFormatter(6)],
      onFieldSubmitted: onSubmitted == null ? null : (_) => onSubmitted!(),
      style: const TextStyle(letterSpacing: 8, fontSize: 20),
      decoration: InputDecoration(
        labelText: 'Kod',
        hintText: '6 haneli kod',
        prefixIcon: const Icon(Icons.pin_outlined),
        border: const OutlineInputBorder(),
        errorText: serverError,
      ),
      validator: (value) => RegExp(r'^\d{6}$').hasMatch(value ?? '') ? null : 'Kod 6 haneli olmalıdır',
    );
  }
}

/// Centred, width-limited column used by the sign-in screens.
class AuthFormBody extends StatelessWidget {
  const AuthFormBody({super.key, required this.formKey, required this.children});

  final GlobalKey<FormState> formKey;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(24, 12, 24, 24),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 420),
            child: Form(
              key: formKey,
              child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: children),
            ),
          ),
        ),
      ),
    );
  }
}

/// A button showing a spinner while [busy].
class BusyButton extends StatelessWidget {
  const BusyButton({super.key, required this.label, required this.busy, required this.onPressed});

  final String label;
  final bool busy;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return FilledButton(
      onPressed: busy ? null : onPressed,
      child: busy
          ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2))
          : Text(label),
    );
  }
}
