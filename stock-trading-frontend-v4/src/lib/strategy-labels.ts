/** 전략·시장·시간대 코드 → 한글 라벨 (백엔드 enum과 동기) */
export const STRATEGY_TYPE_LABELS: Record<string, string> = {
  SUPPLY_SCALPING: '수급단타',
  CLOSING_BET: '종가베팅',
  OPENING_BET: '시가베팅',
  BREAKOUT: '돌파매매',
  PULLBACK: '눌림목',
  SHORT_SWING: '단기스윙',
  NEWS_THEME: '뉴스/테마',
  RISK_EXIT: '위험청산',
}

export const MARKET_CONDITION_LABELS: Record<string, string> = {
  STRONG_BULL: '강한 상승장',
  BULL: '상승장',
  SIDEWAYS: '횡보장',
  WEAK: '약한 장',
  BEAR: '하락장',
  THEME: '테마장',
  LARGE_CAP: '대형주 장세',
  SMALL_CAP: '개별주 장세',
  HIGH_VOLATILITY: '변동성 큰 장',
}

export const MARKET_TIME_WINDOW_LABELS: Record<string, string> = {
  PRE_MARKET: '장 시작 전',
  OPENING_NO_TRADE: '09:00~09:03 관찰',
  OPENING_BET: '09:03~09:20 시가베팅',
  EARLY_MARKET: '09:20~10:30 장초반',
  MORNING_MARKET: '10:30~11:30 오전장',
  LUNCH_MARKET: '11:30~13:00 점심장',
  AFTERNOON_MARKET: '13:00~14:30 오후장',
  CLOSING_PREPARE: '14:30~15:10 종가 후보',
  CLOSING_BET: '15:10~15:20 종가베팅',
  AFTER_MARKET: '장 마감 후',
}

export function strategyLabel(code: string, fallbackName?: string) {
  return fallbackName || STRATEGY_TYPE_LABELS[code] || code
}

export function marketConditionLabel(code: string) {
  return MARKET_CONDITION_LABELS[code] || code
}

export function timeWindowLabel(code: string, fallback?: string) {
  return fallback || MARKET_TIME_WINDOW_LABELS[code] || code
}

export function todayKst(): string {
  return new Date().toLocaleDateString('en-CA', { timeZone: 'Asia/Seoul' })
}
