import 'package:flutter/material.dart';

import '../../../app/app_scope.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/utils/formatters.dart';
import '../data/dashboard_repository.dart';
import '../models/dashboard_summary.dart';

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});

  @override
  State<DashboardScreen> createState() => DashboardScreenState();
}

class DashboardScreenState extends State<DashboardScreen> {
  late final DashboardRepository _repository;
  DashboardSummary? _summary;
  String? _error;

  @override
  void initState() {
    super.initState();
    _repository = DashboardRepository(AppScope.read(context).api);
    reload();
  }

  /// Re-fetches the summary; called by the home shell when this tab is shown.
  Future<void> reload() async {
    try {
      final summary = await _repository.fetchSummary();
      if (!mounted) return;
      setState(() {
        _summary = summary;
        _error = null;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _error = e.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    final user = AppScope.of(context).auth.user;
    final greeting = user == null ? 'Hoş geldin!' : 'Hoş geldin, ${user.name}!';
    final summary = _summary;
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(title: const Text('Ana Sayfa')),
      body: RefreshIndicator(
        onRefresh: reload,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(16),
          children: [
            Text(greeting, style: theme.textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.bold)),
            const SizedBox(height: 16),
            if (summary == null && _error == null)
              const Padding(
                padding: EdgeInsets.only(top: 80),
                child: Center(child: CircularProgressIndicator()),
              )
            else if (summary == null)
              _ErrorCard(message: _error!, onRetry: reload)
            else ...[
              _WeekCard(summary: summary),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: _CountCard(
                      icon: Icons.directions_run,
                      label: 'Toplam koşu',
                      value: '${summary.totalRunCount}',
                      detail: '${formatDecimal(summary.totalDistanceKm)} km',
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: _CountCard(
                      icon: Icons.menu_book,
                      label: 'Kitap',
                      value: '${summary.books.totalCount}',
                      detail: '${summary.books.finishedThisYear} bu yıl bitti',
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: _CountCard(
                      icon: Icons.movie,
                      label: 'Medya',
                      value: '${summary.media.totalCount}',
                      detail: '${summary.media.completedThisYear} bu yıl izlendi',
                    ),
                  ),
                ],
              ),
              if (summary.books.currentlyReading.isNotEmpty) ...[
                const SizedBox(height: 12),
                _ReadingCard(stats: summary.books),
              ],
            ],
          ],
        ),
      ),
    );
  }
}

class _WeekCard extends StatelessWidget {
  const _WeekCard({required this.summary});

  final DashboardSummary summary;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Son 7 gün', style: theme.textTheme.titleMedium),
            const SizedBox(height: 8),
            Text(
              '${formatDecimal(summary.weekDistanceKm)} km',
              style: theme.textTheme.displaySmall?.copyWith(fontWeight: FontWeight.bold),
            ),
            Text(
              summary.weekRunCount == 0
                  ? 'Bu hafta henüz koşmadın'
                  : '${summary.weekRunCount} koşu · ${formatDuration(summary.weekDurationMinutes)}',
              style: theme.textTheme.bodyMedium?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
            const SizedBox(height: 20),
            WeeklyDistanceChart(days: summary.lastSevenDays),
          ],
        ),
      ),
    );
  }
}

/// Minimal bar chart of daily distance; bars scale to the busiest day.
class WeeklyDistanceChart extends StatelessWidget {
  const WeeklyDistanceChart({super.key, required this.days});

  static const double _barAreaHeight = 96;

  final List<DailyDistance> days;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final maxKm = days.fold<double>(0, (max, d) => d.distanceKm > max ? d.distanceKm : max);

    return Row(
      crossAxisAlignment: CrossAxisAlignment.end,
      children: [
        for (final day in days)
          Expanded(
            child: Semantics(
              label: '${formatWeekdayShort(day.date)}: ${formatDecimal(day.distanceKm)} km',
              excludeSemantics: true,
              child: Column(
                children: [
                  if (day.distanceKm > 0)
                    Text(formatDecimal(day.distanceKm), style: theme.textTheme.labelSmall),
                  const SizedBox(height: 4),
                  Container(
                    height: maxKm == 0 ? 4 : 4 + (_barAreaHeight - 4) * day.distanceKm / maxKm,
                    margin: const EdgeInsets.symmetric(horizontal: 6),
                    decoration: BoxDecoration(
                      color: day.distanceKm > 0
                          ? theme.colorScheme.primary
                          : theme.colorScheme.surfaceContainerHighest,
                      borderRadius: const BorderRadius.vertical(top: Radius.circular(4)),
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(formatWeekdayShort(day.date), style: theme.textTheme.labelSmall),
                ],
              ),
            ),
          ),
      ],
    );
  }
}

class _ReadingCard extends StatelessWidget {
  const _ReadingCard({required this.stats});

  final BookStats stats;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final hidden = stats.readingCount - stats.currentlyReading.length;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Şu an okuyorum', style: theme.textTheme.titleMedium),
            for (final book in stats.currentlyReading) ...[
              const SizedBox(height: 12),
              Text(book.title, style: theme.textTheme.bodyLarge, maxLines: 1, overflow: TextOverflow.ellipsis),
              Text(
                [
                  book.author,
                  if (book.currentPage != null && book.pageCount != null) '${book.currentPage}/${book.pageCount} sayfa',
                ].join(' · '),
                style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
              if (book.progress != null)
                Padding(
                  padding: const EdgeInsets.only(top: 6),
                  child: LinearProgressIndicator(value: book.progress, borderRadius: BorderRadius.circular(2)),
                ),
            ],
            if (hidden > 0) ...[
              const SizedBox(height: 12),
              Text('+$hidden kitap daha', style: theme.textTheme.bodySmall),
            ],
          ],
        ),
      ),
    );
  }
}

class _CountCard extends StatelessWidget {
  const _CountCard({required this.icon, required this.label, required this.value, this.detail});

  final IconData icon;
  final String label;
  final String value;
  final String? detail;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, color: theme.colorScheme.primary),
            const SizedBox(height: 8),
            Text(value, style: theme.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold)),
            Text(label, style: theme.textTheme.bodySmall, maxLines: 1, overflow: TextOverflow.ellipsis),
            if (detail != null) Text(detail!, style: theme.textTheme.bodySmall),
          ],
        ),
      ),
    );
  }
}

class _ErrorCard extends StatelessWidget {
  const _ErrorCard({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            const Icon(Icons.cloud_off, size: 40),
            const SizedBox(height: 8),
            Text(message, textAlign: TextAlign.center),
            const SizedBox(height: 8),
            FilledButton.tonal(onPressed: onRetry, child: const Text('Tekrar dene')),
          ],
        ),
      ),
    );
  }
}
