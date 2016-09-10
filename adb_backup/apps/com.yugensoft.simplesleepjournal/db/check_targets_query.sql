SELECT 
	COUNT(*) AS defaults_count
FROM (
SELECT DISTINCT
	 default_targets.time_entry_type,
	 wakeup_or_bedtime
FROM 
	time_entries as default_targets

WHERE
	default_targets.time_entry_type LIKE '%DEFAULT'
)