{{/*
Common labels
*/}}
{{- define "opencrowd.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: opencrowd
app.kubernetes.io/version: {{ .Chart.AppVersion }}
{{- end }}

{{/*
Backend labels
*/}}
{{- define "opencrowd.backend.labels" -}}
{{ include "opencrowd.labels" . }}
app.kubernetes.io/name: opencrowd-backend
app.kubernetes.io/component: backend
{{- end }}

{{/*
Frontend labels
*/}}
{{- define "opencrowd.frontend.labels" -}}
{{ include "opencrowd.labels" . }}
app.kubernetes.io/name: opencrowd-frontend
app.kubernetes.io/component: frontend
{{- end }}
