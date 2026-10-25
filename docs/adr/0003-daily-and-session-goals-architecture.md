# 3. Daily and Session Focus Goals Architecture

Focus Goals are stored with an ISO date (`target_date`: `YYYY-MM-DD`) and an optional `session_id`. Goals planned for a calendar day automatically bind to any focus session occurring on that day, while maintaining live two-way synchronization between the Block Overlay and Dashboard state.

## Considered Options
1. **Session-Only Binding**: Linking goals strictly to a `session_id`. Rejected because users need to plan goals for future or different days before sessions begin.
2. **Profile-Specific Goals**: Attaching goals to specific profiles. Rejected because goals are typically day-specific milestones rather than static profile properties.
3. **Date-Centric with Optional Session Link (Chosen)**: Goals belong to a specific calendar date (`target_date`) and can optionally link to an active `session_id`. All uncompleted goals for the current day automatically present during any active session on that day, and marking a goal complete on the Block Overlay updates the persistent database immediately.
