import { currentPath } from '@/utils';
import { createResource, Suspense } from 'solid-js';

type TeamData = {
  teamName: string;
  teamScore: number;
  teamColor: string;
};

type ScoreLine = {
  name: string;
  team: string;
  kills: number;
  deaths: number;
  assists: number;
  streak: number;
  damage_dealt: number;
  damage_received: number;
  pickups: number;
  throws: number;
  passes: number;
  catches: number;
  strips: number;
  touchdowns: number;
  touchdown_passes: number;
};

type GameData = {
  mapName: string;
  matchTime: string;
  matchTimestamp: string;
  teams: TeamData[];
  stats: ScoreLine[];
};

const getData = async (
  id: string | undefined,
): Promise<GameData | undefined> => {
  if (!id) return undefined;
};

export const App = () => {
  const id = currentPath();

  const [data] = createResource(async () => getData(id));

  return (
    <Suspense>
      <div>hello world!</div>
    </Suspense>
  );
};
