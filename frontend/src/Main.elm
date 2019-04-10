module Main exposing (main)

import Bootstrap.CDN as CDN
import Bootstrap.Grid as Grid
import Bootstrap.Table as Table
import Bootstrap.Utilities.Spacing as Spacing
import Browser
import Html exposing (..)
import Http
import Json.Decode as Decode
import Time
import Url.Builder



---- MODEL ----


type RemoteData e a
    = NotAsked
    | Failure e
    | Success a


type alias Summary =
    { id : String
    , count : Int
    , avg : Float
    , min : Float
    , max : Float
    }


summaryDecoder : Decode.Decoder Summary
summaryDecoder =
    Decode.map5
        Summary
        (Decode.field "id" Decode.string)
        (Decode.field "count" Decode.int)
        (Decode.field "avg" Decode.float)
        (Decode.field "min" Decode.float)
        (Decode.field "max" Decode.float)


type alias Summaries =
    { totalProducts : Int
    , totalReviews : Int
    , reviewSummaries : List Summary
    }


summariesDecoder : Decode.Decoder Summaries
summariesDecoder =
    Decode.map3
        Summaries
        (Decode.field "totalProducts" Decode.int)
        (Decode.field "totalReviews" Decode.int)
        (Decode.field "reviewSummaries" (Decode.list summaryDecoder))


type alias Model =
    { summaries : RemoteData Http.Error Summaries
    , msg : Maybe String
    }


init : ( Model, Cmd Msg )
init =
    ( { summaries = NotAsked, msg = Nothing }, getSummaries )



---- UPDATE ----


getSummaries : Cmd Msg
getSummaries =
    Http.get
        { url = Url.Builder.crossOrigin "http://localhost:8080" [ "summaries" ] []
        , expect = Http.expectJson GetSummariesResult summariesDecoder
        }


type Msg
    = GetSummariesResult (Result Http.Error Summaries)
    | Tick Time.Posix


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GetSummariesResult (Err err) ->
            case model.summaries of
                Success _ ->
                    ( { model | msg = Just "HTTP request failed." }, Cmd.none )

                _ ->
                    ( { model | summaries = Failure err }, Cmd.none )

        GetSummariesResult (Ok result) ->
            ( { model | summaries = Success result, msg = Nothing }, Cmd.none )

        Tick _ ->
            ( model, getSummaries )



---- VIEW ----


viewTable : List Summary -> Html Msg
viewTable summaries =
    let
        body =
            summaries
                |> List.map
                    (\summary ->
                        Table.tr []
                            [ Table.td [] [ text summary.id ]
                            , Table.td [] [ text (String.fromInt summary.count) ]
                            , Table.td [] [ text (String.fromFloat summary.avg) ]
                            , Table.td [] [ text (String.fromFloat summary.min) ]
                            , Table.td [] [ text (String.fromFloat summary.max) ]
                            ]
                    )
                |> Table.tbody []

        header =
            Table.simpleThead
                [ Table.th [] [ text "Id" ]
                , Table.th [] [ text "Count" ]
                , Table.th [] [ text "Avg" ]
                , Table.th [] [ text "Min" ]
                , Table.th [] [ text "Max" ]
                ]
    in
    Table.table { options = [ Table.attr Spacing.mt5 ], thead = header, tbody = body }


viewData : Maybe String -> Summaries -> Html Msg
viewData msg summaries =
    div [ Spacing.mt5 ]
        [ div [] [ text ("Total products: " ++ String.fromInt summaries.totalProducts) ]
        , div [] [ text ("Total reviews: " ++ String.fromInt summaries.totalReviews) ]
        , case msg of
            Just m ->
                text m

            Nothing ->
                text ""
        , viewTable summaries.reviewSummaries
        ]


view : Model -> Html Msg
view model =
    Grid.container []
        [ CDN.stylesheet -- creates an inline style node with the Bootstrap CSS
        , Grid.row []
            [ Grid.col []
                (h1 [] [ text "Reviews Summaries" ]
                    :: (case model.summaries of
                            NotAsked ->
                                [ text "No data..." ]

                            Failure _ ->
                                [ text "Error while loading data..." ]

                            Success summaries ->
                                [ viewData model.msg summaries ]
                       )
                )
            ]
        ]



---- PROGRAM ----


subscriptions : Model -> Sub Msg
subscriptions _ =
    Time.every 1000 Tick


main : Program () Model Msg
main =
    Browser.element
        { view = view
        , init = \_ -> init
        , update = update
        , subscriptions = subscriptions
        }
