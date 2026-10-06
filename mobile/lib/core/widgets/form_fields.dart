import 'package:flutter/material.dart';

import '../utils/formatters.dart';

/// Optional date picker field (no time); dates after today can't be picked.
class FormDateField extends StatelessWidget {
  const FormDateField({
    super.key,
    required this.label,
    required this.value,
    required this.onChanged,
    this.icon = Icons.event,
    this.emptyText = 'Seçilmedi',
    this.serverError,
  });

  final String label;
  final DateTime? value;
  final ValueChanged<DateTime?> onChanged;
  final IconData icon;

  /// Shown when no date is picked.
  final String emptyText;
  final String? serverError;

  Future<void> _pick(BuildContext context) async {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final current = value;
    final picked = await showDatePicker(
      context: context,
      initialDate: current == null || current.isAfter(today) ? today : current,
      firstDate: DateTime(1900),
      lastDate: today,
    );
    if (picked != null) onChanged(picked);
  }

  @override
  Widget build(BuildContext context) {
    final current = value;
    return InkWell(
      onTap: () => _pick(context),
      borderRadius: BorderRadius.circular(4),
      child: InputDecorator(
        decoration: InputDecoration(
          labelText: label,
          prefixIcon: Icon(icon),
          suffixIcon: current == null
              ? const Icon(Icons.edit_calendar_outlined)
              : IconButton(
                  tooltip: 'Tarihi temizle',
                  icon: const Icon(Icons.close),
                  onPressed: () => onChanged(null),
                ),
          border: const OutlineInputBorder(),
          errorText: serverError,
        ),
        child: Text(
          current == null ? emptyText : formatDate(current),
          style: current == null ? TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant) : null,
        ),
      ),
    );
  }
}

/// 1–5 star rating; tapping the current rating clears it.
class RatingField extends StatelessWidget {
  const RatingField({super.key, required this.value, required this.onChanged, this.serverError});

  final int? value;
  final ValueChanged<int?> onChanged;
  final String? serverError;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return InputDecorator(
      decoration: InputDecoration(
        labelText: 'Puan (opsiyonel)',
        prefixIcon: const Icon(Icons.star_outline),
        border: const OutlineInputBorder(),
        errorText: serverError,
      ),
      child: Row(
        children: [
          for (var i = 1; i <= 5; i++)
            IconButton(
              tooltip: '$i yıldız',
              visualDensity: VisualDensity.compact,
              onPressed: () => onChanged(value == i ? null : i),
              icon: Icon(
                (value ?? 0) >= i ? Icons.star : Icons.star_border,
                color: (value ?? 0) >= i ? Colors.amber.shade700 : theme.colorScheme.outline,
              ),
            ),
        ],
      ),
    );
  }
}

/// e.g. 4 -> "★★★★☆"
String formatStars(int rating) => '★' * rating + '☆' * (5 - rating);
