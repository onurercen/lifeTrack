import 'package:flutter/material.dart';

import '../../../app/app_scope.dart';
import '../../../core/utils/formatters.dart';
import '../../../core/widgets/entity_list_screen.dart';
import '../data/run_repository.dart';
import '../models/run.dart';
import 'run_form_screen.dart';

class RunningScreen extends StatelessWidget {
  const RunningScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final repository = RunRepository(AppScope.read(context).api);
    return EntityListScreen<Run>(
      title: 'Koşu',
      addLabel: 'Koşu ekle',
      emptyIcon: Icons.directions_run,
      emptyText: 'Henüz koşu eklemedin.\nİlk koşunu eklemek için aşağıdaki butona dokun.',
      load: (_, __, page) => repository.fetchRuns(page: page),
      delete: (run) => repository.deleteRun(run.id),
      idOf: (run) => run.id,
      deletePrompt: (run) => '${formatDecimal(run.distanceKm)} km koşu silinsin mi?',
      headerBuilder: (generation) => _RunSummary(key: ValueKey(generation), repository: repository),
      itemBuilder: (context, run, onTap) => _RunTile(run: run, onTap: onTap),
      formBuilder: (run) => RunFormScreen(repository: repository, run: run),
    );
  }
}

/// Totals over all runs; the list only holds the pages loaded so far.
/// Rebuilt with a new key after every list change, which loads them again.
class _RunSummary extends StatefulWidget {
  const _RunSummary({super.key, required this.repository});

  final RunRepository repository;

  @override
  State<_RunSummary> createState() => _RunSummaryState();
}

class _RunSummaryState extends State<_RunSummary> {
  late final Future<RunSummary> _summary = widget.repository.fetchSummary();

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<RunSummary>(
      future: _summary,
      builder: (context, snapshot) {
        final summary = snapshot.data;
        // Placeholders while loading, and if the totals can't be loaded.
        return _buildCard(
          count: summary == null ? '—' : '${summary.totalCount}',
          totalKm: summary == null ? '—' : formatDecimal(summary.totalDistanceKm),
          pace: summary == null || summary.totalDistanceKm == 0
              ? '—'
              : formatPace(summary.totalDurationMinutes / summary.totalDistanceKm),
        );
      },
    );
  }

  Widget _buildCard({required String count, required String totalKm, required String pace}) {
    return Card(
      margin: const EdgeInsets.all(16),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 16),
        child: Row(
          children: [
            _Stat(label: 'Koşu', value: count),
            _Stat(label: 'Toplam km', value: totalKm),
            _Stat(label: 'Ort. tempo', value: pace),
          ],
        ),
      ),
    );
  }
}

class _Stat extends StatelessWidget {
  const _Stat({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Expanded(
      child: Column(
        children: [
          Text(value, style: theme.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold)),
          const SizedBox(height: 4),
          Text(label, style: theme.textTheme.bodySmall),
        ],
      ),
    );
  }
}

class _RunTile extends StatelessWidget {
  const _RunTile({required this.run, required this.onTap});

  final Run run;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final details = [
      formatDuration(run.durationMinutes),
      formatPace(run.paceMinPerKm),
      if (run.caloriesBurned != null) '${run.caloriesBurned} kcal',
    ].join(' · ');
    return ListTile(
      onTap: onTap,
      leading: const CircleAvatar(child: Icon(Icons.directions_run)),
      title: Text('${formatDecimal(run.distanceKm)} km'),
      subtitle: Text(
        [details, formatDateTime(run.runAt), if (run.notes != null) run.notes!].join('\n'),
      ),
      isThreeLine: true,
    );
  }
}
