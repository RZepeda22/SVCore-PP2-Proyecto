import { statusInfo } from '../constants';

export function StatusBadge({ status }) {
    const info = statusInfo(status);
    return <span className={'status-badge ' + info.badge}>{info.label}</span>;
}

export function TypeBadge({ shortName }) {
    return <span className={'type-badge ' + String(shortName).toLowerCase()}>{shortName}</span>;
}
