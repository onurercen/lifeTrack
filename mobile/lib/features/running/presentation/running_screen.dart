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
      load: (_) => repository.fetchRuns(),
      delete: (run) => repository.deleteRun(run.id),
      idOf: (run) => run.id,
      deletePrompt: (run) => '${formatDecimal(run.distanceKm)} km koşu silinsin mi?',
      headerBuilder: (runs) => _RunSummary(runs: runs),
      itemBuilder: (context, run, onTap) => _RunTile(run: run, onTap: onTap),
      formBuilder: (run) => RunFormScreen(repository: repository, run: run),
    );
  }
}

class _RunSummary extends StatelessWidget {
  const _RunSummary({required this.runs});

  final List<Run> runs;

  @override
  Widget build(BuildContext context) {
    final totalKm = runs.fold<double>(0, (sum, r) => sum + r.distanceKm);
    final totalMinutes = runs.fold<int>(0, (sum, r) => sum + r.durationMinutes);

    return Card(
      margin: const EdgeInsets.all(16),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 16),
        child: Row(
          children: [
            _Stat(label: 'Koşu', value: '${runs.length}'),
            _Stat(label: 'Toplam km', value: formatDecimal(totalKm)),
            _Stat(label: 'Ort. tempo', value: totalKm == 0 ? '—' : formatPace(totalMinutes / totalKm)),
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
