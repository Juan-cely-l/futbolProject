import { useState, useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { fetchSyncLeagues, fetchSyncSeasons } from '../api/external'
import { useSync } from '../context/SyncContext'

export default function SyncModal({ open, onClose }) {
  const { startSync, syncStatus, isPending, progress } = useSync()
  const [selectedLeagues, setSelectedLeagues] = useState([])
  const [selectedSeason, setSelectedSeason] = useState(null)
  const [showProgress, setShowProgress] = useState(false)
  const [expandedTeam, setExpandedTeam] = useState(null)
  const [limitEnabled, setLimitEnabled] = useState(false)
  const [maxTeams, setMaxTeams] = useState(20)

  const { data: leagues } = useQuery({
    queryKey: ['sync-leagues'],
    queryFn: fetchSyncLeagues,
    enabled: open,
  })

  const { data: seasons } = useQuery({
    queryKey: ['sync-seasons'],
    queryFn: fetchSyncSeasons,
    enabled: open,
  })

  useEffect(() => {
    if (open && leagues && leagues.length > 0 && selectedLeagues.length === 0) {
      setSelectedLeagues([leagues[0].id])
    }
    if (open && seasons && selectedSeason === null) {
      setSelectedSeason(seasons.currentSeason)
    }
  }, [open, leagues, seasons, selectedLeagues.length, selectedSeason])

  useEffect(() => {
    if (syncStatus === 'syncing') setShowProgress(true)
  }, [syncStatus])

  const toggleLeague = (id) => {
    setSelectedLeagues((prev) =>
      prev.includes(id) ? prev.filter((l) => l !== id) : [...prev, id],
    )
  }

  const handleStart = () => {
    if (selectedLeagues.length === 0) return
    setShowProgress(true)
    const resolvedMaxTeams = limitEnabled ? Math.max(1, maxTeams) : null
    startSync(selectedLeagues, selectedSeason, resolvedMaxTeams)
  }

  const handleClose = () => {
    if (isPending) return
    setShowProgress(false)
    onClose()
  }

  if (!open) return null

  const isDone = syncStatus === 'success' || syncStatus === 'error'

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="bg-gray-800 rounded-lg p-6 w-full max-w-lg mx-4 shadow-xl border border-gray-700 max-h-[90vh] flex flex-col">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-bold text-white">
            {showProgress ? 'Sync Progress' : 'Sync Data'}
          </h2>
          {!isPending && (
            <button
              onClick={handleClose}
              className="text-gray-400 hover:text-white text-xl leading-none"
            >
              &times;
            </button>
          )}
        </div>

        {showProgress ? (
          /* ── Progress view ── */
          <div className="space-y-4 overflow-y-auto">
            <div className="text-sm text-gray-300 space-y-1">
              {progress.totalLeagues > 0 && (
                <div className="flex justify-between">
                  <span>Leagues</span>
                  <span className="font-mono">{progress.processedLeagues} / {progress.totalLeagues}</span>
                </div>
              )}
              <div className="flex justify-between">
                <span>Teams</span>
                <span className="font-mono">{progress.processedTeams} / {progress.totalTeams}</span>
              </div>
              <div className="flex justify-between">
                <span>Players created</span>
                <span className="font-mono">{progress.playersCreated}</span>
              </div>
            </div>

            {isPending && (
              <div className="w-full bg-gray-700 rounded-full h-2">
                <div
                  className="bg-lime-400 h-2 rounded-full transition-all duration-500"
                  style={{
                    width: progress.totalTeams > 0
                      ? `${Math.min(100, (progress.processedTeams / progress.totalTeams) * 100)}%`
                      : '0%',
                  }}
                />
              </div>
            )}

            {isDone && (
              <>
                {progress.teams && progress.teams.length > 0 ? (
                  <div className="space-y-1">
                    <h3 className="text-sm font-semibold text-gray-400 uppercase tracking-wider">
                      Synced Teams ({progress.teams.length})
                    </h3>
                    <div className="space-y-1 max-h-64 overflow-y-auto">
                      {progress.teams.map((team, idx) => (
                        <div key={idx} className="bg-gray-700/50 rounded border border-gray-700">
                          <button
                            onClick={() => setExpandedTeam(expandedTeam === idx ? null : idx)}
                            className="w-full flex items-center justify-between px-3 py-2 text-left"
                          >
                            <div className="flex items-center gap-2 min-w-0">
                              <span className={`text-xs transition-transform ${expandedTeam === idx ? 'rotate-90' : ''}`}>
                                &#9654;
                              </span>
                              <span className="text-sm text-white font-medium truncate">{team.name}</span>
                              {team.country && (
                                <span className="text-xs text-gray-500 truncate">{team.country}</span>
                              )}
                            </div>
                            <span className={`text-xs font-semibold px-2 py-0.5 rounded-full shrink-0 ${
                              team.created
                                ? 'bg-green-900/50 text-green-400'
                                : 'bg-blue-900/50 text-blue-400'
                            }`}>
                              {team.created ? 'CREATED' : 'UPDATED'}
                            </span>
                          </button>
                          {expandedTeam === idx && (
                            <div className="border-t border-gray-700">
                              {team.players && team.players.length > 0 ? (
                                <div className="divide-y divide-gray-700">
                                  {team.players.map((player, pIdx) => (
                                    <div key={pIdx} className="flex items-center gap-3 px-3 py-1.5 text-xs">
                                      <span className="text-gray-200 w-28 truncate" title={player.name}>
                                        {player.name}
                                      </span>
                                      {player.position && (
                                        <span className={`w-20 text-center font-medium ${
                                          player.position === 'GOALKEEPER' ? 'text-gk' :
                                          player.position === 'DEFENDER' ? 'text-def' :
                                          player.position === 'MIDFIELDER' ? 'text-mid' :
                                          player.position === 'FORWARD' ? 'text-fwd' : 'text-gray-400'
                                        }`}>
                                          {player.position.charAt(0) + player.position.slice(1).toLowerCase()}
                                        </span>
                                      )}
                                      {player.age != null && (
                                        <span className="text-gray-500 w-6 text-center">{player.age}</span>
                                      )}
                                      {player.goals != null && (
                                        <span className="text-gray-400 w-5 text-center">{player.goals}G</span>
                                      )}
                                      {player.assists != null && (
                                        <span className="text-gray-400 w-5 text-center">{player.assists}A</span>
                                      )}
                                      {player.valueMarket != null && (
                                        <span className="text-gray-400 w-16 text-right font-mono">
                                          {player.valueMarket >= 1_000_000
                                            ? `${(player.valueMarket / 1_000_000).toFixed(1)}M`
                                            : `${(player.valueMarket / 1_000).toFixed(0)}K`}
                                        </span>
                                      )}
                                    </div>
                                  ))}
                                </div>
                              ) : (
                                <p className="text-xs text-gray-500 px-3 py-2">No players in squad</p>
                              )}
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                ) : (
                  <p className="text-sm text-gray-500 text-center">No teams synced</p>
                )}

                <button
                  onClick={handleClose}
                  className="w-full py-2 rounded bg-lime-500 text-gray-900 font-semibold hover:bg-lime-400 transition"
                >
                  Done
                </button>
              </>
            )}
          </div>
        ) : (
          /* ── Configuration view ── */
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Leagues</label>
              <div className="space-y-2 max-h-48 overflow-y-auto">
                {leagues?.map((league) => (
                  <label
                    key={league.id}
                    className="flex items-center gap-2 cursor-pointer text-gray-200 hover:text-white"
                  >
                    <input
                      type="checkbox"
                      checked={selectedLeagues.includes(league.id)}
                      onChange={() => toggleLeague(league.id)}
                      className="accent-lime-400"
                    />
                    {league.name}
                  </label>
                ))}
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Season</label>
              {seasons && (
                <select
                  value={selectedSeason ?? ''}
                  onChange={(e) => setSelectedSeason(Number(e.target.value))}
                  className="w-full bg-gray-700 text-white border border-gray-600 rounded px-3 py-2"
                >
                  {Array.from(
                    { length: seasons.maxSeason - seasons.minSeason + 1 },
                    (_, i) => seasons.maxSeason - i,
                  ).map((year) => (
                    <option key={year} value={year}>
                      {year}/{String(year + 1).slice(2)}
                    </option>
                  ))}
                </select>
              )}
            </div>

            <div>
              <label className="flex items-center gap-2 cursor-pointer text-gray-200 hover:text-white">
                <input
                  type="checkbox"
                  checked={limitEnabled}
                  onChange={(e) => setLimitEnabled(e.target.checked)}
                  className="accent-lime-400"
                />
                <span className="text-sm font-medium">Limit teams per league</span>
              </label>
              {limitEnabled && (
                <div className="flex items-center gap-2 mt-1 ml-6">
                  <input
                    type="number"
                    min={1}
                    value={maxTeams}
                    onChange={(e) => setMaxTeams(Math.max(1, Number(e.target.value)))}
                    className="w-24 bg-gray-700 text-white border border-gray-600 rounded px-3 py-1.5 text-sm"
                  />
                  <span className="text-xs text-gray-400">max teams</span>
                </div>
              )}
            </div>

            <button
              onClick={handleStart}
              disabled={isPending || selectedLeagues.length === 0}
              className="w-full py-2 rounded bg-lime-500 text-gray-900 font-semibold hover:bg-lime-400 transition disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isPending ? 'Starting...' : `Sync ${selectedLeagues.length} league${selectedLeagues.length !== 1 ? 's' : ''}`}
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
