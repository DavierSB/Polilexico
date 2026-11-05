create table public.profiles (
  id uuid primary key references auth.users on delete cascade,
  username text not null check (char_length(username) between 3 and 20),
  username_key text generated always as (lower(username)) stored,
  country char(2) not null check (country ~ '^[A-Z]{2}$'),
  created_at timestamptz not null default now(),
  unique (username_key, country)
);

create table public.games (
  id bigint generated always as identity primary key,
  mode text not null check (mode in ('classic', 'duplicate')),
  settings jsonb not null default '{}',
  started_at timestamptz,
  ended_at timestamptz not null default now()
);

create table public.game_players (
  game_id bigint not null references public.games on delete cascade,
  user_id uuid references public.profiles on delete set null,
  name text not null,
  score int not null,
  result text not null check (result in ('win', 'loss', 'draw')),
  place int not null,
  best_bingo int not null default 0,
  best_play int not null default 0,
  efficiency real,
  primary key (game_id, name)
);

create index game_players_user on public.game_players (user_id);

create table public.backups (
  user_id uuid primary key references public.profiles on delete cascade,
  data text not null,
  updated_at timestamptz not null default now()
);

create table public.photos (
  user_id uuid primary key references public.profiles on delete cascade,
  data text not null,
  updated_at timestamptz not null default now()
);

create view public.player_stats with (security_invoker = true) as
select
  p.user_id,
  g.mode,
  count(*)::int as games,
  (count(*) filter (where p.result = 'win'))::int as wins,
  (count(*) filter (where p.result = 'loss'))::int as losses,
  (count(*) filter (where p.result = 'draw'))::int as draws,
  max(p.best_bingo)::int as best_bingo,
  max(p.best_play)::int as best_play,
  avg(p.score)::float8 as avg_score,
  avg(p.efficiency)::float8 as avg_efficiency
from public.game_players p
join public.games g on g.id = p.game_id
where p.user_id is not null
group by p.user_id, g.mode;

alter table public.profiles enable row level security;
alter table public.games enable row level security;
alter table public.game_players enable row level security;
alter table public.backups enable row level security;
alter table public.photos enable row level security;

create policy "Perfiles públicos" on public.profiles for select using (true);
create policy "Partidas públicas" on public.games for select using (true);
create policy "Resultados públicos" on public.game_players for select using (true);

grant select on public.player_stats to anon, authenticated;
grant all on public.profiles, public.games, public.game_players, public.backups, public.photos, public.player_stats to service_role;
grant usage, select on all sequences in schema public to service_role;

create table public.adjourned_games (
  id text primary key,
  player_a uuid not null references public.profiles on delete cascade,
  player_b uuid not null references public.profiles on delete cascade,
  name_a text not null,
  name_b text not null,
  country_a char(2) not null,
  country_b char(2) not null,
  time_ms bigint not null default 0,
  overtime_ms bigint not null default 0,
  void boolean not null default false,
  untimed boolean not null default false,
  save text not null,
  created_at timestamptz not null default now()
);

create index adjourned_games_a on public.adjourned_games (player_a);
create index adjourned_games_b on public.adjourned_games (player_b);
alter table public.adjourned_games enable row level security;
